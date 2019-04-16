package update3_2;
//package com.huawei;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.apache.log4j.Logger;

public class Main {
	private static final Logger logger = Logger.getLogger(Main.class);
	public static void main(String[] args) throws Exception {

		// if (args.length != 4) {
		// logger.error("please input args: inputFilePath, resultFilePath");
		// return;
		// }

		logger.info("Start...");
		// String carPath = args[0];
		// String roadPath = args[1];
		// String crossPath = args[2];
		// String answerPath = args[3];
		// String presetAnswerPath = args[4];

		String carPath = "src/car.txt";
		String roadPath = "src/road.txt";
		String crossPath = "src/cross.txt";
		String answerPath = "src/answer.txt";
		String presetAnswerPath = "src/presetAnswer.txt";
		logger.info("carPath = " + carPath + " roadPath = " + roadPath + " crossPath = " + crossPath
				+ " and answerPath = " + answerPath);

		// TODO:read input files
		logger.info("start read input files");
		// readTxt from car.txt --> carInfo List
		List<Integer[]> carList = readTxt(carPath);
		// readTxt from road.txt --> roadInfo List
		List<Integer[]> roadList = readTxt(roadPath);
		// readTxt from cross.txt --> crossInfo List
		List<Integer[]> crossList = readTxt(crossPath);
		// readTxt from answer.txt --> answer List
		List<Integer[]> answerList = readTxt(answerPath);
		// readTxt from presetAnswer.txt --> presetAnsList List
		List<Integer[]> presetAnsList = readTxt(presetAnswerPath);
		// change presetAnsewer road to crossId
		List<List<Integer>> presetAnswerList = changeRoadToCrossId(presetAnsList, roadList, crossList);
		List<List<Integer>> AnswerList = changeRoadToCrossId(answerList, roadList, crossList);

		// TODO: calc
		// create carInfos Array
		List<CarInfo> carInfos = new ArrayList<CarInfo>();
		for (int i = 0; i < carList.size(); i++) {
			// create CarInfo according to CarInfo constructor
			CarInfo creatCarInfo = new CarInfo(carList.get(i)[0], carList.get(i)[3], carList.get(i)[4],
					carList.get(i)[1], carList.get(i)[2], "home", carList.get(i)[5], carList.get(i)[6]);
			carInfos.add(creatCarInfo);
		}
		for (int i = 0; i < carInfos.size(); i++) {
			CarInfo car1 = carInfos.get(i);
			if (carInfos.get(i).getCarPreset() == 1) {
				List<Integer> list = FindMethod.findCarPath(car1.getCarId(), presetAnswerList);
				car1.setCarPlanTime(list.get(0));
				car1.carPath = list.subList(1, list.size());
			}else {
				List<Integer> list = FindMethod.findCarPath(car1.getCarId(), AnswerList);
				car1.setCarPlanTime(list.get(0));
				car1.carPath = list.subList(1, list.size());
			}
			
		}
		// sort car according to car's priority(high --> low) planTime(low -->
		// high) carId(low --> high)
		int priInitialTime = carInfos.get(0).getCarPlanTime();
		// create roadInfos Array
		List<RoadInfo> roadInfos = new ArrayList<RoadInfo>();
		for (int i = 0; i < roadList.size(); i++) {
			// create RoadInfo according to RoadInfo constructor
			RoadInfo creatRoadInfo = new RoadInfo(roadList.get(i)[0], roadList.get(i)[1], roadList.get(i)[2],
					roadList.get(i)[3], roadList.get(i)[4], roadList.get(i)[5], roadList.get(i)[6]);
			roadInfos.add(creatRoadInfo);
		}
		// create crossInfos Array
		List<CrossInfo> crossInfos = new ArrayList<CrossInfo>();
		for (int i = 0; i < crossList.size(); i++) {
			RoadInfo crossRoadTop = FindMethod.findRoad(roadInfos, crossList.get(i)[1], carList);
			RoadInfo crossRoadRight = FindMethod.findRoad(roadInfos, crossList.get(i)[2], carList);
			RoadInfo crossRoadBottom = FindMethod.findRoad(roadInfos, crossList.get(i)[3], carList);
			RoadInfo crossRoadLeft = FindMethod.findRoad(roadInfos, crossList.get(i)[4], carList);
			// create CrossInfo according to CrossInfo constructor
			CrossInfo creatCrossInfo = new CrossInfo(crossList.get(i)[0], crossRoadTop, crossRoadRight, crossRoadBottom,
					crossRoadLeft);
			crossInfos.add(creatCrossInfo);
		}
		//set cars to each road
		for (int i = 0; i < carInfos.size(); i++) {
			CarInfo car1 = carInfos.get(i);
			car1.setStateCar("wait");
			car1.setCarFrom();
			car1.setCarTo();
			car1.setNowRoad(roadList, roadInfos, carList);
			car1.setStateCross(roadList, roadInfos, crossInfos, carList);
			if (car1.getNowRoad() != null) {
				car1.getNowRoad().getRoadArrayWaitCars().add(car1);
			}else if (car1.getNowRoadTurn() != null){
				car1.getNowRoadTurn().getRoadArrayTurnWaitCars().add(car1);
			}
		}
		//sort each road's cars as priority time Id
		for (int i = 0; i < roadInfos.size(); i++) {
			RoadInfo roadInfo = roadInfos.get(i);
			SortCarPriTimeId sortCarPriTimeId2 = new SortCarPriTimeId();
			Collections.sort(roadInfo.getRoadArrayWaitCars(), sortCarPriTimeId2);
			
			if (roadInfo.getIsDuplex() == 1) {
				Collections.sort(roadInfo.getRoadArrayTurnWaitCars(), sortCarPriTimeId2);
			}
		}
		// initial time
		Integer time = 0;
		// initial endCarArray
		List<CarInfo> carResultInfos = new ArrayList<CarInfo>();
		logger.info("start run");
		while (true) {
			Boolean deadLockBoolean = false;
			// end state cars'nums
			// all roads'car run
			waitRoadCars(roadInfos, carInfos, time, roadList, crossInfos);
			// set priority cars to roads
			waitInitialCars(carInfos, time, roadInfos, roadList, crossInfos, carList, true);
			List<CarInfo> preWaitCars = new ArrayList<CarInfo>();
			// create all roads'carSequence
			createCarSequence(roadInfos);
			List<CarInfo> waitCars = new ArrayList<CarInfo>();
			do {
				// initial wait cars array
				waitCars = new ArrayList<CarInfo>();
				// for each cross Traversal
				for (int i = 0; i < crossInfos.size(); i++) {
					CrossInfo crossInfo = crossInfos.get(i);
					Iterator<Map.Entry<Integer, Map<RoadInfo, Integer>>> iterIn = crossInfo.getCrossMapIn().entrySet()
							.iterator();
					// for each road Traversal
					while (iterIn.hasNext()) {
						Map.Entry<Integer, Map<RoadInfo, Integer>> entryIn = iterIn.next();
						Iterator<Map.Entry<RoadInfo, Integer>> iterRoadIn = entryIn.getValue().entrySet().iterator();
						RoadInfo roadInfo = null;
						Integer RightOrTurn = 0;
						while (iterRoadIn.hasNext()) {
							Map.Entry<RoadInfo, Integer> entryRoadIn = iterRoadIn.next();
							roadInfo = entryRoadIn.getKey();
							RightOrTurn = entryRoadIn.getValue();
						}
						// find out-cross-road
						CarInfo[][] roadCarsArray = null;
						// find out-cross-roadSequence
						List<CarInfo> roadCarsSequence = new ArrayList<CarInfo>();
						switch (RightOrTurn) {
						case 0:
							roadCarsArray = roadInfo.getRoadArray();
							break;

						case 1:
							roadCarsArray = roadInfo.getRoadTurnArray();
							break;
						}
						// roadSequence Traversal
						while ((roadCarsSequence = getRoadCarsSequence(roadInfo, RightOrTurn, roadCarsSequence)) != null
								&& roadCarsSequence.size() > 0) {
							CarInfo carInfo = roadCarsSequence.get(0);
							int nullNowPosition = 0;
							int roadNowSpeed = roadInfo.getRoadSpeed();
							int carSpeed = carInfo.getCarSpeed();
							int speedNowMin = Math.min(roadNowSpeed, carSpeed);
							int preCar = 0;
							List<Integer> carPosition = FindMethod.findCarPosition(carInfo, roadCarsArray);
							
							for (int j = carPosition.get(1) + 1; j < roadCarsArray[0].length; j++) {
								if (roadCarsArray[carPosition.get(0)][j] == null) {
									nullNowPosition += 1;
								} else {
									preCar = j;
									break;
								}
							}
							if ((nullNowPosition < speedNowMin) && (preCar == 0)) {
								
								if (!conflict(carInfo, crossInfo, roadInfo)) {
									break;
								}
								
								if (setCarToNextRoad(crossInfo, roadInfo, roadCarsArray, carPosition, carInfo,
										nullNowPosition, time, carResultInfos, carInfos, roadInfos, roadList,
										crossInfos, carList)) {
									channelCarsRun(roadCarsArray, carPosition.get(0), roadNowSpeed);
									roadInfo.createSequence();
									// time: 621 allTime: 2718676
									waitChannelInitialCars(time, roadInfos, roadList, crossInfos, carList,true, roadCarsArray, roadInfo);
								} else {
									break;
								}
							}
						}

					}
				}
				//calc all roads' wait car numbers
				for (int i = 0; i < roadInfos.size(); i++) {
					for (int j = 0; j < roadInfos.get(i).getRoadArray().length; j++) {
						for (int j2 = 0; j2 < roadInfos.get(i).getRoadArray()[j].length; j2++) {
							if (roadInfos.get(i).getRoadArray()[j][j2] != null
									&& roadInfos.get(i).getRoadArray()[j][j2].getStateCar() == "wait") {
								waitCars.add(roadInfos.get(i).getRoadArray()[j][j2]);
							}
						}
					}
					if (roadInfos.get(i).getRoadTurnArray() != null) {
						for (int j = 0; j < roadInfos.get(i).getRoadTurnArray().length; j++) {
							for (int j2 = 0; j2 < roadInfos.get(i).getRoadTurnArray()[j].length; j2++) {
								if (roadInfos.get(i).getRoadTurnArray()[j][j2] != null
										&& roadInfos.get(i).getRoadTurnArray()[j][j2].getStateCar() == "wait") {
									waitCars.add(roadInfos.get(i).getRoadTurnArray()[j][j2]);
								}

							}
						}
					}
				}
				//judge deadLock
				Integer deadLock = 0;
				for (int i = 0; i < waitCars.size(); i++) {
					if (preWaitCars.contains(waitCars.get(i))) {
						deadLock += 1;
					}
				}
				if (preWaitCars.size() > 1 && deadLock == preWaitCars.size()) {
					deadLockBoolean = true;
					System.out.println("------deadLock------");
					break;
				} else {
					deadLock = 0;
				}
				preWaitCars.removeAll(preWaitCars);
				for (int i = 0; i < waitCars.size(); i++) {
					preWaitCars.add(waitCars.get(i));
				}
			} while (waitCars.size() != 0);
			//set all cars to roads
			waitInitialCars(carInfos, time, roadInfos, roadList, crossInfos, carList, false);
			if (carResultInfos.size() == carList.size()) {
				break;
			}
			time += 1;
		}
		// TODO: write answer.txt
		logger.info("Start calc scheduleTime and allScheduleTime");
		List<Integer[]> carListResult = readTxt(carPath);
		SortCarListId sortCarListId = new SortCarListId();
		Collections.sort(carListResult, sortCarListId);
		for (int i = 0; i < carResultInfos.size(); i++) {
			for (int j = 0; j < carListResult.size(); j++) {
				if (carListResult.get(j)[0].equals(carResultInfos.get(i).getCarId())) {
					carResultInfos.get(i).setCarPlanTime(carListResult.get(j)[4]);
				}
			}
			
		}
		outPutTime(priInitialTime, time, carResultInfos, carListResult);
		logger.info("End...");

	}
	// calc scheduleTime allScheduleTime
	private static void outPutTime(int priInitialTime, Integer time, List<CarInfo> carResultInfos,
			List<Integer[]> carListResult) {
		int allTime = 0;
		int preAllTime = 0;
		int maxPreTime = 0;
		SortCarId sortCarId = new SortCarId();
		Collections.sort(carResultInfos, sortCarId);
		for (int i = 0; i < carResultInfos.size(); i++) {
			allTime = allTime + carResultInfos.get(i).getCarEndTime() - carListResult.get(i)[4];
			if (carResultInfos.get(i).getCarPriority() == 1) {
				preAllTime = preAllTime + carResultInfos.get(i).getCarEndTime() - carListResult.get(i)[4];
				maxPreTime = Math.max(maxPreTime, carResultInfos.get(i).getCarEndTime());
			}
		}
		//calc a and b
		int allCarLength = carResultInfos.size();
		int priCarLength = 0;
		int allCarMaxSpeed = 0;
		int allCarMinSpeed = Integer.MAX_VALUE;
		int priCarMaxSpeed = 0;
		int priCarMinSpeed = Integer.MAX_VALUE;
		int allCarMaxTime = 0;
		int allCarMinTime = Integer.MAX_VALUE;
		int priCarMaxTime = 0;
		int priCarMinTime = Integer.MAX_VALUE;
		for (int i = 0; i < carResultInfos.size(); i++) {
			if (carResultInfos.get(i).getCarPriority() == 1) {
				priCarLength += 1;
				priCarMaxSpeed = Math.max(priCarMaxSpeed, carResultInfos.get(i).getCarSpeed());
				priCarMinSpeed = Math.min(priCarMinSpeed, carResultInfos.get(i).getCarSpeed());
				priCarMaxTime = Math.max(priCarMaxTime, carResultInfos.get(i).getCarPlanTime());
				priCarMinTime = Math.min(priCarMinTime, carResultInfos.get(i).getCarPlanTime());
			}
			allCarMaxSpeed = Math.max(allCarMaxSpeed, carResultInfos.get(i).getCarSpeed());
			allCarMinSpeed = Math.min(allCarMinSpeed, carResultInfos.get(i).getCarSpeed());
			allCarMaxTime = Math.max(allCarMaxTime, carResultInfos.get(i).getCarPlanTime());
			allCarMinTime = Math.min(allCarMinTime, carResultInfos.get(i).getCarPlanTime());
		}
		Set<Integer> allCarbegNum = new HashSet<Integer>();
		Set<Integer> priCarbegNum = new HashSet<Integer>();
		Set<Integer> allCarendNum = new HashSet<Integer>();
		Set<Integer> priCarendNum = new HashSet<Integer>();
		int minPreTime = Integer.MAX_VALUE;
		for (int i = 0; i < carListResult.size(); i++) {
			allCarbegNum.add(carListResult.get(i)[1]);
			allCarendNum.add(carListResult.get(i)[2]);
			if (carListResult.get(i)[5] == 1) {
				minPreTime = Math.min(minPreTime, carListResult.get(i)[4]);
				priCarbegNum.add(carListResult.get(i)[1]);
				priCarendNum.add(carListResult.get(i)[2]);
			}
		}
		double a1 = (double)allCarLength /priCarLength;
		BigDecimal a1BigDecimal = new BigDecimal(a1);
		a1 = a1BigDecimal.setScale(5, BigDecimal.ROUND_HALF_UP).doubleValue();
		double a21 = (double)allCarMaxSpeed/allCarMinSpeed;
		BigDecimal a21BigDecimal = new BigDecimal(a21);
		a21 = a21BigDecimal.setScale(5, BigDecimal.ROUND_HALF_UP).doubleValue();
		double a22 = (double)priCarMaxSpeed/priCarMinSpeed;
		BigDecimal a22BigDecimal = new BigDecimal(a22);
		a22 = a22BigDecimal.setScale(5, BigDecimal.ROUND_HALF_UP).doubleValue();
		double a2 = a21/a22;
		BigDecimal a2BigDecimal = new BigDecimal(a2);
		a2 = a2BigDecimal.setScale(5, BigDecimal.ROUND_HALF_UP).doubleValue();
		double a31 = (double)allCarMaxTime/allCarMinTime;
		BigDecimal a31BigDecimal = new BigDecimal(a31);
		a31 = a31BigDecimal.setScale(5, BigDecimal.ROUND_HALF_UP).doubleValue();
		double a32 = (double)priCarMaxTime/priCarMinTime;
		BigDecimal a32BigDecimal = new BigDecimal(a32);
		a32 = a32BigDecimal.setScale(5, BigDecimal.ROUND_HALF_UP).doubleValue();
		double a3 = a31/a32;
		BigDecimal a3BigDecimal = new BigDecimal(a3);
		a3 = a3BigDecimal.setScale(5, BigDecimal.ROUND_HALF_UP).doubleValue();
		double a4 = (double)allCarbegNum.size()/priCarbegNum.size();
		BigDecimal a4BigDecimal = new BigDecimal(a4);
		a4 = a4BigDecimal.setScale(5, BigDecimal.ROUND_HALF_UP).doubleValue();
		double a5 = (double)allCarendNum.size()/priCarendNum.size();
		BigDecimal a5BigDecimal = new BigDecimal(a5);
		a5 = a5BigDecimal.setScale(5, BigDecimal.ROUND_HALF_UP).doubleValue();
		double a = a1 * 0.05 + a2 * 0.2375 + a3 * 0.2375+ a4 * 0.2375+ a5 * 0.2375;
		double TE = a * (maxPreTime - minPreTime) + time;
		double b = a1 * 0.8 + a2 * 0.05 + a3 * 0.05 + a4 * 0.05 + a5 * 0.05;
		double TEsum = b * (preAllTime) + allTime;
		System.out.println("a: " + a + " b: " + b);
		System.out.println("PreTime: " + (maxPreTime - minPreTime) + " preAllTime: " + preAllTime);
		System.out.println("time: " + time + " allTime: " + allTime);
		System.out.println("CodeCraftJudge end schedule time: " + Math.round(TE) + " allScheduleTime: " + Math.round(TEsum));
	}
	//set each road sequence
	private static List<CarInfo> getRoadCarsSequence(RoadInfo roadInfo, Integer RightOrTurn,
			List<CarInfo> roadCarsSequence) {
		switch (RightOrTurn) {
		case 0:
			roadCarsSequence = roadInfo.getRoadArraySequence();
			break;
		case 1:
			roadCarsSequence = roadInfo.getRoadTurnArraySequence();
			break;
		}
		return roadCarsSequence;
	}
	//set priority cars to now cross' out road
	private static void waitChannelInitialCars(Integer time, List<RoadInfo> roadInfos,
			List<Integer[]> roadList, List<CrossInfo> crossInfos, List<Integer[]> carList, boolean priority,
			CarInfo[][] roadCarsArray, RoadInfo roadInfo) {
		List<CarInfo> roadWaitCars = new ArrayList<CarInfo>();
		if (roadInfo.getRoadArray().equals(roadCarsArray)) {
			roadWaitCars = roadInfo.getRoadArrayWaitCars();
		}else if (roadInfo.getRoadTurnArray().equals(roadCarsArray)){
			roadWaitCars = roadInfo.getRoadArrayTurnWaitCars();
		}
		for (int i = 0; i < roadWaitCars.size(); i++) {
			CarInfo car1 = roadWaitCars.get(i);
			Integer planTime = 0;
			if (car1 == null) {
				continue;
			}
			planTime = car1.getCarPlanTime();
			if (priority) {
				if (car1.getCarPriority() != 1) {
					break;
				}else if(planTime > time){
					break;
				}
			}
			if (planTime <= time) {
				waitEachRoadInitialCars(planTime, roadInfo, roadWaitCars, i, car1, roadCarsArray);
			}
		}
	}
	//set car to next road
	private static Boolean setCarToNextRoad(CrossInfo crossInfo, RoadInfo roadInfo, CarInfo[][] roadCarsArray,
			List<Integer> carPosition, CarInfo carInfo, int nullNowPosition, Integer time, List<CarInfo> carResultInfos,
			List<CarInfo> carInfos, List<RoadInfo> roadInfos, List<Integer[]> roadList, List<CrossInfo> crossInfos,
			List<Integer[]> carList) {
		RoadInfo roadNextInfo = carInfo.getNextRoad();
		Integer roadNextSpeed;
		if (roadNextInfo == null) {
			carEndTo(time, carResultInfos, roadCarsArray, carPosition.get(1), carPosition.get(0));
			return true;
		}
		roadNextSpeed = roadNextInfo.getRoadSpeed();
		int speedNextMin = Math.min(carInfo.getCarSpeed(), roadNextSpeed);
		int nextRoadRunMax = 0;
		nextRoadRunMax = speedNextMin - nullNowPosition;
		if (nextRoadRunMax <= 0) {
			nextRoadRunMax = 0;
		}
		if (nextRoadRunMax == 0) {
			runNowRoadTop(roadCarsArray, carPosition.get(1), carPosition.get(0), nullNowPosition);
			return true;
		}
		Boolean result = false;
		Integer rightOrTurn = crossInfo.getCrossMapOut().get(roadNextInfo.getRoadId()).get(roadNextInfo);
		CarInfo[][] roadNextCarInfos = null;
		switch (rightOrTurn) {
		case 0:
			roadNextCarInfos = roadNextInfo.getRoadArray();
			break;

		case 1:
			roadNextCarInfos = roadNextInfo.getRoadTurnArray();
			break;
		}
		for (int i = 0; i < roadNextCarInfos.length; i++) {
			int preNullPosition = 0;
			for (int j = 0; j < nextRoadRunMax; j++) {
				if (roadNextCarInfos[i][j] == null) {
					preNullPosition += 1;
				} else if (roadNextCarInfos[i][j].getStateCar().equals("stop")) {
					result = true;
					break;
				} else if (roadNextCarInfos[i][j].getStateCar().equals("wait")) {
					result = false;
					break;
				}
			}
			if (result == true && i < roadNextCarInfos.length - 1 && preNullPosition == 0) {
				continue;
			}
			if (preNullPosition == nextRoadRunMax) {
				roadNextCarInfos[i][nextRoadRunMax - 1] = roadCarsArray[carPosition.get(0)][carPosition.get(1)];
				roadCarsArray[carPosition.get(0)][carPosition.get(1)] = null;
				roadNextCarInfos[i][nextRoadRunMax - 1].setStateCar("stop");
				setCarNewInfo(roadNextCarInfos[i][nextRoadRunMax - 1], roadList, roadInfos, crossInfos, carList);
				return true;
			} else if (result == true) {
				if (i == roadNextCarInfos.length - 1 && preNullPosition == 0) {
					if (nullNowPosition == 0) {
						roadCarsArray[carPosition.get(0)][carPosition.get(1)].setStateCar("stop");
						return true;
					} else {
						runNowRoadTop(roadCarsArray, carPosition.get(1), carPosition.get(0), nullNowPosition);
						return true;
					}
				} else {
					roadNextCarInfos[i][preNullPosition - 1] = roadCarsArray[carPosition.get(0)][carPosition.get(1)];
					roadCarsArray[carPosition.get(0)][carPosition.get(1)] = null;
					roadNextCarInfos[i][preNullPosition - 1].setStateCar("stop");
					setCarNewInfo(roadNextCarInfos[i][preNullPosition - 1], roadList, roadInfos, crossInfos, carList);
					return true;
				}
			} else {
				return false;
			}
		}
		return result;
	}
	//judge if the current car is in conflict
	private static Boolean conflict(CarInfo carInfo, CrossInfo crossInfo, RoadInfo roadInfo) {
		for (int i = 0; i < crossInfo.getCrossRoadIn().size(); i++) {
			if (roadInfo.equals(crossInfo.getCrossRoadIn().get(i))) {
				continue;
			}
			RoadInfo roadInfoOther = crossInfo.getCrossRoadIn().get(i);
			if (roadInfoOther != null) {
				int rightOrTurn = crossInfo.getCrossMapIn().get(roadInfoOther.getRoadId()).get(roadInfoOther);
				CarInfo[][] roadOtherCarsArray = null;
				List<CarInfo> roadOtherCarsSequence = new ArrayList<CarInfo>();
				switch (rightOrTurn) {
					case 0:
						roadOtherCarsArray = roadInfoOther.getRoadArray();
						roadOtherCarsSequence = roadInfoOther.getRoadArraySequence();
						break;
	
					case 1:
						roadOtherCarsArray = roadInfoOther.getRoadTurnArray();
						roadOtherCarsSequence = roadInfoOther.getRoadTurnArraySequence();
						break;
				}
				if (roadOtherCarsSequence != null && roadOtherCarsSequence.size() > 0) {
					CarInfo carInfoOther = roadOtherCarsSequence.get(0);
					int nullPerPosition = 0;
					int roadPerSpeed = roadInfoOther.getRoadSpeed();
					int carSpeed = carInfoOther.getCarSpeed();
					int speedPerMin = Math.min(roadPerSpeed, carSpeed);
					int preCar = 0;
					List<Integer> carPosition = FindMethod.findCarPosition(carInfoOther, roadOtherCarsArray);
					for (int j = carPosition.get(1) + 1; j < roadOtherCarsArray[0].length; j++) {
						if (roadOtherCarsArray[carPosition.get(0)][j] == null) {
							nullPerPosition += 1;
						} else {
							preCar = j;
							break;
						}
					}
					if ((nullPerPosition < speedPerMin) && (preCar == 0)) {
						if (carInfo.getNextRoad() != null && carInfoOther.getNextRoad() != null) {
							if (carInfo.getNextRoad().equals(carInfoOther.getNextRoad())) {
								if (carInfo.getCarPriority() < carInfoOther.getCarPriority()) {
									return false;
								}else if (carInfo.getCarPriority() == carInfoOther.getCarPriority()) {
									if (carInfo.getStateCross().equals("left") && carInfoOther.getStateCross().equals("ahead")) {
										return false;
									}else if (carInfo.getStateCross().equals("right") && carInfoOther.getStateCross().equals("ahead")) {
										return false;
									}else if (carInfo.getStateCross().equals("right") && carInfoOther.getStateCross().equals("left")) {
										return false;
									}
								}
							}
						
						}else if (carInfo.getNextRoad() != null && (carInfoOther.getNextRoad() == null)) {
							switch (i) {
							case 0:
								if (carInfo.getNextRoad().equals(crossInfo.getCrossRoadIn().get(2))) {
									if (carInfo.getCarPriority() < carInfoOther.getCarPriority()) {
										return false;
									}else if (carInfo.getCarPriority() == carInfoOther.getCarPriority()) {
										if (carInfo.getStateCross().equals("right") || carInfo.getStateCross().equals("left")) {
											return false;
										}
									}
								}
								break;
							case 1:
								if (carInfo.getNextRoad().equals(crossInfo.getCrossRoadIn().get(3))) {
									if (carInfo.getCarPriority() < carInfoOther.getCarPriority()) {
										return false;
									}else if (carInfo.getCarPriority() == carInfoOther.getCarPriority()) {
										if (carInfo.getStateCross().equals("right") || carInfo.getStateCross().equals("left")) {
											return false;
										}
									}
								}
								break;
							case 2:
								if (carInfo.getNextRoad().equals(crossInfo.getCrossRoadIn().get(0))) {
									if (carInfo.getCarPriority() < carInfoOther.getCarPriority()) {
										return false;
									}else if (carInfo.getCarPriority() == carInfoOther.getCarPriority()) {
										if (carInfo.getStateCross().equals("right") || carInfo.getStateCross().equals("left")) {
											return false;
										}
									}
								}
								break;
							case 3:
								if (carInfo.getNextRoad().equals(crossInfo.getCrossRoadIn().get(1))) {
									if (carInfo.getCarPriority() < carInfoOther.getCarPriority()) {
										return false;
									}else if (carInfo.getCarPriority() == carInfoOther.getCarPriority()) {
										if (carInfo.getStateCross().equals("right") || carInfo.getStateCross().equals("left")) {
											return false;
										}
									}
								}
								break;
							}
						}else if (carInfo.getNextRoad() == null && (carInfoOther.getNextRoad() != null)) {
							switch (crossInfo.getCrossRoadIn().indexOf(roadInfo)) {
							case 0:
								if (carInfoOther.getNextRoad().equals(crossInfo.getCrossRoadIn().get(2))) {
									if (carInfo.getCarPriority() < carInfoOther.getCarPriority()) {
										return false;
									}
								}
								break;
							case 1:
								if (carInfoOther.getNextRoad().equals(crossInfo.getCrossRoadIn().get(3))) {
									if (carInfo.getCarPriority() < carInfoOther.getCarPriority()) {
										return false;
									}
								}
								break;
							case 2:
								if (carInfoOther.getNextRoad().equals(crossInfo.getCrossRoadIn().get(0))) {
									if (carInfo.getCarPriority() < carInfoOther.getCarPriority()) {
										return false;
									}
								}
								break;
							case 3:
								if (carInfoOther.getNextRoad().equals(crossInfo.getCrossRoadIn().get(1))) {
									if (carInfo.getCarPriority() < carInfoOther.getCarPriority()) {
										return false;
									}
								}
								break;
							}
						}
					}
				}
			}
		}
		return true;
	}
	
	//for each road --> create road sequence
	private static void createCarSequence(List<RoadInfo> roadInfos) {
		// TODO Auto-generated method stub
		
		for (int i = 0; i < roadInfos.size(); i++) {
			RoadInfo roadInfo = roadInfos.get(i);
			roadInfo.createSequence();
		}

	}
	
	// change road to cross Id
	private static List<List<Integer>> changeRoadToCrossId(List<Integer[]> presetAnswerList, List<Integer[]> roadList,
			List<Integer[]> crossList) {
		// TODO Auto-generated method stub
		List<List<Integer>> result = new ArrayList<List<Integer>>();
		for (int i = 0; i < presetAnswerList.size(); i++) {
			Integer[] carPath = presetAnswerList.get(i);
			List<Integer> carNewPath = new ArrayList<Integer>();
			carNewPath.add(carPath[0]);
			carNewPath.add(carPath[1]);
			for (int j = 2; j < carPath.length - 1; j++) {
				for (int j2 = 0; j2 < crossList.size(); j2++) {
					if (Arrays.asList(crossList.get(j2)).contains(carPath[j])
							&& Arrays.asList(crossList.get(j2)).contains(carPath[j + 1])) {
						carNewPath.add(crossList.get(j2)[0]);
					}
				}
			}
			for (int j = 0; j < crossList.size(); j++) {
				if (Arrays.asList(crossList.get(j)).contains(carPath[2])
						&& !Arrays.asList(crossList.get(j)).contains(carPath[3])) {
					carNewPath.add(2, crossList.get(j)[0]);
				}
				if (Arrays.asList(crossList.get(j)).contains(carPath[carPath.length - 1])
						&& !Arrays.asList(crossList.get(j)).contains(carPath[carPath.length - 2])) {
					carNewPath.add(crossList.get(j)[0]);
				}
			}
			result.add(carNewPath);
		}
		return result;
	}

	private static void runNowRoadTop(CarInfo[][] roadCarsArray, int k2, int k, int nullNowPosition) {
		//set car to this channel Top position
		roadCarsArray[k][k2 + nullNowPosition] = roadCarsArray[k][k2];
		roadCarsArray[k][k2] = null;
		roadCarsArray[k][k2 + nullNowPosition].setStateCar("stop");
	}

	private static void carEndTo(Integer time, List<CarInfo> carResultInfos, CarInfo[][] roadCarsArray, int k2, int k) {
		//car to end and add this car to the resultList
		roadCarsArray[k][k2].setCarEndTime(time);
		roadCarsArray[k][k2].setStateCar("stop");
		roadCarsArray[k][k2].carPathAct.add(roadCarsArray[k][k2].getCarFrom());
		roadCarsArray[k][k2].carPathAct.add(roadCarsArray[k][k2].getCarTo());
		carResultInfos.add(roadCarsArray[k][k2]);
		roadCarsArray[k][k2] = null;
	}

	public static List<Integer> turnRoad(List<Integer> list, List<Integer[]> crossList, List<Integer[]> roadList) {
		//change cross to road Id
		List<Integer> list1 = new ArrayList<Integer>();
		HashMap<Integer, HashMap<Integer, Integer>> roadMap = new HashMap<Integer, HashMap<Integer, Integer>>();
		for (int i = 0; i < crossList.size(); i++) {
			HashMap<Integer, Integer> step = new HashMap<Integer, Integer>();
			for (int j = 0; j < roadList.size(); j++) {
				if (roadList.get(j)[4].equals(crossList.get(i)[0])) {
					step.put(roadList.get(j)[5], roadList.get(j)[0]);
				} else if (roadList.get(j)[6] == 1 && roadList.get(j)[5].equals(crossList.get(i)[0])) {
					step.put(roadList.get(j)[4], roadList.get(j)[0]);
				}
			}
			roadMap.put(crossList.get(i)[0], step);
		}
		for (int i = 0; i < list.size() - 1; i++) {
			list1.add(roadMap.get(list.get(i)).get(list.get(i + 1)));
		}
		return list1;
	}

	private static void channelCarsRun(CarInfo[][] road, int k, Integer roadNowSpeed) {
		//single channel's cars run
		for (int l = road[k].length - 1; l >= 0; l--) {
			if (road[k][l] != null && road[k][l].getStateCar() == "wait") {
				Integer carSpeed = road[k][l].getCarSpeed();
				Integer nullNowRoad = 0;
				int preCarNowRoad = 0;
				for (int m = l + 1; m < road[k].length; m++) {
					if (road[k][m] == null) {
						nullNowRoad += 1;
					} else {
						preCarNowRoad = m;
						break;
					}
				}
				int speedNowRoadMin = Math.min(carSpeed, roadNowSpeed);
				if (nullNowRoad >= speedNowRoadMin) {
					road[k][l].setStateCar("stop");
					road[k][l + speedNowRoadMin] = road[k][l];
					road[k][l] = null;
				} else if ((nullNowRoad < speedNowRoadMin) && preCarNowRoad != 0) {
					if (road[k][preCarNowRoad].getStateCar().equals("wait")) {
						road[k][l].setStateCar("wait");
					} else if (nullNowRoad == 0) {
						road[k][l].setStateCar("stop");
					} else {
						road[k][l].setStateCar("stop");
						road[k][l + nullNowRoad] = road[k][l];
						road[k][l] = null;
					}
				}
			}
		}
	}

	private static void setCarNewInfo(CarInfo carInfo, List<Integer[]> roadList, List<RoadInfo> roadInfos,
			List<CrossInfo> crossInfos, List<Integer[]> carList) {
		//when this car across the cross, update it's state
		carInfo.changeCarPath();
		carInfo.setCarFrom();
		carInfo.setCarTo();
		carInfo.setNowRoad(roadList, roadInfos, carList);
		carInfo.setStateCross(roadList, roadInfos, crossInfos, carList);

	}
	//begin all roads' cars run
	private static void waitRoadCars(List<RoadInfo> roadInfos, List<CarInfo> carInfos, int time,
			List<Integer[]> roadList, List<CrossInfo> crossInfos) {
		
		for (int i = 0; i < roadInfos.size(); i++) {
			RoadInfo roadInfo = roadInfos.get(i);
			CarInfo[][] carArrays  = roadInfos.get(i).getRoadArray();
			carsStateInitialRoad(roadInfo, carArrays);
			if (roadInfos.get(i).getIsDuplex() == 1) {
				carArrays  = roadInfos.get(i).getRoadTurnArray();
				carsStateInitialRoad(roadInfo, carArrays);
			}
		}
	}
	//begin single road's cars run
	private static void carsStateInitialRoad(RoadInfo roadInfo, CarInfo[][] carArrays) {
		for (int j = 0; j < carArrays.length; j++) {
			for (int j2 = carArrays[j].length - 1; j2 >= 0; j2--) {
				CarInfo carInfoRoad = carArrays[j][j2];
				if (carInfoRoad != null) {
					Integer carSpeed = carInfoRoad.getCarSpeed();
					Integer roadSpeed = roadInfo.getRoadSpeed();
					int nullPosition = 0;
					int preCarPosition = 0;
					for (int k = j2 + 1; k < carArrays[j].length; k++) {
						if (carArrays[j][k] == null) {
							nullPosition += 1;
						} else {
							preCarPosition = k;
							break;
						}
					}
					int speedMin = Math.min(carSpeed, roadSpeed);
					if ((nullPosition < speedMin) && (preCarPosition == 0)) {
						carInfoRoad.setStateCar("wait");
					} else if ((nullPosition >= speedMin)) {
						carInfoRoad.setStateCar("stop");
						carArrays[j][j2 + speedMin] = carArrays[j][j2];
						carArrays[j][j2] = null;

					} else if ((nullPosition < speedMin) && preCarPosition != 0) {
						if (carArrays[j][preCarPosition].getStateCar().equals("wait")) {
							carInfoRoad.setStateCar("wait");
						} else if (nullPosition == 0) {
							carInfoRoad.setStateCar("stop");
						} else {
							carInfoRoad.setStateCar("stop");
							carArrays[j][j2 + nullPosition] = carArrays[j][j2];
							carArrays[j][j2] = null;
						}
					}
				}
			}
		}
	}
	//read txtInfo and change it to List<Integer[]>
	public static List<Integer[]> readTxt(String str) throws Exception {
		File file = new File(str);
		FileReader fileReader = new FileReader(file);
		BufferedReader bufferedReader = new BufferedReader(fileReader);
		String line = bufferedReader.readLine();
		String[] strbeg = line.split("");
		if (strbeg[0].equals("#")) {
			line = bufferedReader.readLine();
		}
		List<Integer[]> list1 = new ArrayList<Integer[]>();
		while (line != null) {
			String[] str1 = line.split(",");
			str1[0] = str1[0].substring(1, str1[0].length());
			str1[str1.length - 1] = str1[str1.length - 1].substring(0, str1[str1.length - 1].length() - 1);
			for (int i = 0; i < str1.length; i++) {
				if (str1[i].substring(0, 1).equals(" ")) {
					str1[i] = str1[i].substring(1, str1[i].length());
				}
			}
			Integer[] str2 = new Integer[str1.length];
			for (int i = 0; i < str1.length; i++) {
				str2[i] = Integer.parseInt(str1[i]);
			}
			list1.add(str2);
			line = bufferedReader.readLine();
		}
		bufferedReader.close();
		fileReader.close();
		return list1;
	}
	
	//set cars to all roads
	private static void waitInitialCars(List<CarInfo> carInfos, Integer time, List<RoadInfo> roadInfos,
			List<Integer[]> roadList, List<CrossInfo> crossInfos, List<Integer[]> carList, Boolean priority)
			throws Exception {
		
		for (int i = 0; i < roadInfos.size(); i++) {
			RoadInfo roadInfo = roadInfos.get(i);
			List<CarInfo> carInfosRoad = roadInfos.get(i).getRoadArrayWaitCars();
			for (int j = 0; j < carInfosRoad.size(); j++) {
				CarInfo car1 = carInfosRoad.get(j);
				
				if (car1 == null) {
					continue;
				}
				Integer planTime = car1.getCarPlanTime();
				if (priority) {
					if (car1.getCarPriority() != 1) {
						break;
					}else if(planTime > time){
						break;
					}
				}
				if (planTime <= time) {
					CarInfo[][] roadArray = roadInfo.getRoadArray();
					//set cars to each road
					waitEachRoadInitialCars(time, roadInfo, carInfosRoad, j, car1, roadArray);
				}
				
			}
			if (roadInfo.getIsDuplex() == 1) {
				carInfosRoad = roadInfos.get(i).getRoadArrayTurnWaitCars();
				for (int j = 0; j < carInfosRoad.size(); j++) {
					CarInfo car1 = carInfosRoad.get(j);
					if (car1 == null) {
						continue;
					}
					Integer planTime = car1.getCarPlanTime();
					if (priority) {
						if (car1.getCarPriority() != 1) {
							break;
						}else if(planTime > time){
							break;
						}
					}
					if (planTime <= time) {
						CarInfo[][] roadArray = roadInfo.getRoadTurnArray();
						//set cars to each road
						waitEachRoadInitialCars(time, roadInfo, carInfosRoad, j, car1, roadArray);
					}
					
					
				}
			}
			
			
		}
	}
	//set cars to each road
	private static void waitEachRoadInitialCars(Integer time, RoadInfo roadInfo, List<CarInfo> carInfosRoad, int j,
			CarInfo car1, CarInfo[][] roadArray) {
		for (int t = 0; t < roadInfo.getRoadChannel(); t++) {
			if (roadArray[t][0] == null) {
				int nullPosition = 0;
				int preCar = 0;
				for (int k = 1; k < Math.min(roadInfo.getRoadSpeed(), car1.getCarSpeed()); k++) {
					if (roadArray[t][k] == null) {
						nullPosition += 1;
					} else {
						preCar = k;
						break;
					}
				}
				if (nullPosition == Math.min(roadInfo.getRoadSpeed(), car1.getCarSpeed()) - 1) {
					roadArray[t][nullPosition] = car1;
					carInfosRoad.set(j, null);
					roadArray[t][nullPosition].setCarActTime(time);
					roadArray[t][nullPosition].setStateCar("stop");
					break;
				} else if (preCar != 0
						&& roadArray[t][preCar].getStateCar().equals("stop")) {
					roadArray[t][nullPosition] = car1;
					carInfosRoad.set(j, null);
					roadArray[t][nullPosition].setCarActTime(time);
					roadArray[t][nullPosition].setStateCar("stop");
					break;
				} else if (preCar != 0
						&& roadArray[t][preCar].getStateCar().equals("wait")){
					break;
				}
			}else if (roadArray[t][0] != null && roadArray[t][0].getStateCar().equals("wait")) {
				break;
			}
		}
	}
}
class FindMethod {
	// find road in roadInfos (return RoadInfo)
	public static RoadInfo findRoad(List<RoadInfo> roadInfos, Integer roadId, List<Integer[]> carList) {
		for (int i = 0; i < roadInfos.size(); i++) {
			if (roadInfos.get(i).getRoadId().equals(roadId)) {
				return roadInfos.get(i);
			}
		}
		return null;
	}
	// find car's position in this road
	public static List<Integer> findCarPosition(CarInfo carInfo, CarInfo[][] roadCarsArray) {
		// TODO Auto-generated method stub
		List<Integer> carPosition = new ArrayList<Integer>();
		for (int ver = roadCarsArray[0].length - 1; ver >= 0; ver--) {
			for (int lev = 0; lev < roadCarsArray.length; lev++) {
				if (roadCarsArray[lev][ver] != null && roadCarsArray[lev][ver].equals(carInfo)) {
					carPosition.add(lev);
					carPosition.add(ver);
				}
			}
		}
		return carPosition;
	}

	// find carEndTo in carList (return Integer)
	public static Integer findCar(List<Integer[]> carList, Integer CarId) {
		for (int i = 0; i < carList.size(); i++) {
			if (carList.get(i)[0].equals(CarId)) {
				return carList.get(i)[2];
			}
		}
		return null;
	}

	// find cross in crossInfos (return CrossInfo)
	public static CrossInfo findCross(List<CrossInfo> crossInfos, Integer crossId) {
		for (int i = 0; i < crossInfos.size(); i++) {
			if (crossInfos.get(i).getCrossId().equals(crossId)) {
				return crossInfos.get(i);
			}
		}
		return null;
	}

	// return preset car path list
	public static List<Integer> findCarPath(Integer carId, List<List<Integer>> presetAnswerList) {
		List<Integer> carPathIntegers = new ArrayList<Integer>();
		for (int i = 0; i < presetAnswerList.size(); i++) {
			if (presetAnswerList.get(i).get(0).equals(carId)) {
				carPathIntegers = presetAnswerList.get(i).subList(1, presetAnswerList.get(i).size());
			}
		}
		return carPathIntegers;
	}
}

class SortCarPriTimeId implements Comparator<CarInfo> {
	@Override
	public int compare(CarInfo o1, CarInfo o2) {
		// TODO Auto-generated method stub
		if (o1.getCarPriority() > o2.getCarPriority()) {
			return -1;
		} else if (o1.getCarPriority() < o2.getCarPriority()) {
			return 1;
		} else {
			if (o1.getCarPlanTime() < o2.getCarPlanTime()) {
				return -1;
			} else if (o1.getCarPlanTime() > o2.getCarPlanTime()) {
				return 1;
			} else {
				if (o1.getCarId() < o2.getCarId()) {
					return -1;
				} else if (o1.getCarId() > o2.getCarId()) {
					return 1;
				} else {
					return 0;
				}
			}
		}

	}

}

class SortCarId implements Comparator<CarInfo> {
	@Override
	public int compare(CarInfo o1, CarInfo o2) {
		// TODO Auto-generated method stub

		if (o1.getCarId() < o2.getCarId()) {
			return -1;
		} else if (o1.getCarId() > o2.getCarId()) {
			return 1;
		} else {
			return 0;
		}

	}

}

class SortCarListId implements Comparator<Integer[]> {
	@Override
	public int compare(Integer[] o1, Integer[] o2) {
		// TODO Auto-generated method stub
		if (o1[0] < o2[0]) {
			return -1;
		} else if (o1[0] > o2[0]) {
			return 1;
		} else {
			return 0;
		}
	}
}

class CrossInfo {
	private Integer crossId;
	private RoadInfo crossRoadTop;
	private RoadInfo crossRoadRight;
	private RoadInfo crossRoadBottom;
	private RoadInfo crossRoadLeft;
	private List<RoadInfo> crossRoadIn;
	Map<Integer, Map<RoadInfo, Integer>> crossMapIn;// roadId RoadInfo turn or right
	Map<Integer, Map<RoadInfo, Integer>> crossMapOut;

	public CrossInfo(Integer crossId, RoadInfo crossRoadTop, RoadInfo crossRoadRight, RoadInfo crossRoadBottom,
			RoadInfo crossRoadLeft) {
		super();
		this.crossId = crossId;
		this.crossRoadTop = crossRoadTop;
		this.crossRoadRight = crossRoadRight;
		this.crossRoadBottom = crossRoadBottom;
		this.crossRoadLeft = crossRoadLeft;
		this.crossRoadIn = new ArrayList<RoadInfo>();
		this.crossRoadIn.add(this.getCrossRoadTop());
		this.crossRoadIn.add(this.getCrossRoadRight());
		this.crossRoadIn.add(this.getCrossRoadBottom());
		this.crossRoadIn.add(this.getCrossRoadLeft());
		crossMapIn = new TreeMap<Integer, Map<RoadInfo, Integer>>();
		crossMapOut = new TreeMap<Integer, Map<RoadInfo, Integer>>();
		for (int i = 0; i < this.crossRoadIn.size(); i++) {
			if (this.crossRoadIn.get(i) != null) {
				Map<RoadInfo, Integer> map1 = new LinkedHashMap<RoadInfo, Integer>();
				Map<RoadInfo, Integer> map2 = new LinkedHashMap<RoadInfo, Integer>();
				if (this.crossId.equals(this.crossRoadIn.get(i).getRoadFrom())) {
					map1.put(this.crossRoadIn.get(i), 1);// turn
					this.crossMapIn.put(this.crossRoadIn.get(i).getRoadId(), map1);
					map2.put(this.crossRoadIn.get(i), 0);// right
					this.crossMapOut.put(this.crossRoadIn.get(i).getRoadId(), map2);
				} else if (this.crossId.equals(this.crossRoadIn.get(i).getRoadTo())) {
					map1.put(this.crossRoadIn.get(i), 0);
					this.crossMapIn.put(this.crossRoadIn.get(i).getRoadId(), map1);
					map2.put(this.crossRoadIn.get(i), 1);
					this.crossMapOut.put(this.crossRoadIn.get(i).getRoadId(), map2);
				}
			}
		}
	}

	public Map<Integer, Map<RoadInfo, Integer>> getCrossMapIn() {
		return crossMapIn;
	}

	public void setCrossMapIn(Map<Integer, Map<RoadInfo, Integer>> crossMapIn) {
		this.crossMapIn = crossMapIn;
	}

	public Map<Integer, Map<RoadInfo, Integer>> getCrossMapOut() {
		return crossMapOut;
	}

	public void setCrossMapOut(Map<Integer, Map<RoadInfo, Integer>> crossMapOut) {
		this.crossMapOut = crossMapOut;
	}

	public List<RoadInfo> getCrossRoadIn() {
		return crossRoadIn;
	}

	public void setCrossRoadIn(List<RoadInfo> crossRoadIn) {
		this.crossRoadIn = crossRoadIn;
	}

	public Integer getCrossId() {
		return crossId;
	}

	public void setCrossId(Integer crossId) {
		this.crossId = crossId;
	}

	public RoadInfo getCrossRoadTop() {
		return crossRoadTop;
	}

	public void setCrossRoadTop(RoadInfo crossRoadTop) {
		this.crossRoadTop = crossRoadTop;
	}

	public RoadInfo getCrossRoadRight() {
		return crossRoadRight;
	}

	public void setCrossRoadRight(RoadInfo crossRoadRight) {
		this.crossRoadRight = crossRoadRight;
	}

	public RoadInfo getCrossRoadBottom() {
		return crossRoadBottom;
	}

	public void setCrossRoadBottom(RoadInfo crossRoadBottom) {
		this.crossRoadBottom = crossRoadBottom;
	}

	public RoadInfo getCrossRoadLeft() {
		return crossRoadLeft;
	}

	public void setCrossRoadLeft(RoadInfo crossRoadLeft) {
		this.crossRoadLeft = crossRoadLeft;
	}

}

class CarInfo {
	private Integer carId;
	public List<Integer> carPath;
	public List<Integer> carPathAct = new ArrayList<Integer>();
	private Integer carFrom;
	private Integer carTo;
	private Integer carSpeed;
	public Integer carPlanTime;
	private Integer carActTime;
	private String stateCross;
	private RoadInfo nowRoad;
	private RoadInfo nowRoadTurn;
	private String stateCar;
	private RoadInfo nextRoad;
	private Integer carEndTime;
	private Integer carPriority;
	private Integer carPreset;

	public Integer getCarPriority() {
		return carPriority;
	}

	public void setCarPriority(Integer carPriority) {
		this.carPriority = carPriority;
	}

	public Integer getCarPreset() {
		return carPreset;
	}

	public void setCarPreset(Integer carPreset) {
		this.carPreset = carPreset;
	}

	public Integer getCarEndTime() {
		return carEndTime;
	}

	public void setCarEndTime(Integer carEndTime) {
		this.carEndTime = carEndTime;
	}

	public RoadInfo getNextRoad() {
		return nextRoad;
	}

	public void setNewCarPath(List<Integer> setPath) {
		// TODO Auto-generated method stub
		this.carPathAct.add(this.carPath.get(0));
		List<Integer> path = new ArrayList<Integer>();
		// path.add(this.carPath.get(0));
		path.addAll(setPath);
		this.carPath = path;
	}

	public void setNextRoad(RoadInfo nextRoad) {
		this.nextRoad = nextRoad;
	}

	public List<Integer> getCarPathAct() {
		return carPathAct;
	}

	public void setCarPathAct(List<Integer> carPathAct) {
		this.carPathAct = carPathAct;
	}

	public String getStateCar() {
		return stateCar;
	}

	public void setStateCar(String stateCar) {
		this.stateCar = stateCar;
	}

	Boolean complete = false;

	public CarInfo(Integer carId, Integer carSpeed, Integer carPlanTime, Integer carFrom, Integer carTo,
			String stateCar, Integer priority, Integer preset) {
		super();
		this.carId = carId;
		this.carSpeed = carSpeed;
		this.carPlanTime = carPlanTime;
		this.carFrom = carFrom;
		this.carTo = carTo;
		this.stateCar = stateCar;
		this.carPriority = priority;
		this.carPreset = preset;
	}

	public Integer getCarId() {
		return carId;
	}

	public void setCarId(Integer carId) {
		this.carId = carId;
	}

	public List<Integer> getCarPath() {
		return carPath;
	}

	public void setCarPath(List<Integer> carPath){
		this.carPath = carPath;
	}

	public void changeCarPath() {
		this.carPathAct.add(this.carPath.get(0));
		this.carPath.remove(0);
	}

	public Integer getCarFrom() {
		return carFrom;
	}

	public void setCarFrom() {
		this.carFrom = this.carPath.get(0);
	}

	public Integer getCarTo() {
		return carTo;
	}

	public void setCarTo() {
		if (this.carPath.size() >= 2) {
			this.carTo = this.carPath.get(1);
		} else {
			this.carTo = -1;
		}
	}

	public Integer getCarSpeed() {
		return carSpeed;
	}

	public void setCarSpeed(Integer carSpeed) {
		this.carSpeed = carSpeed;
	}

	public Integer getCarPlanTime() {
		return carPlanTime;
	}

	public void setCarPlanTime(Integer carPlanTime) {
		this.carPlanTime = carPlanTime;
	}

	public Integer getCarActTime() {
		return carActTime;
	}

	public void setCarActTime(Integer carActTime) {
		this.carActTime = carActTime;
	}

	public String getStateCross() {
		return stateCross;
	}

	public void setStateCross(List<Integer[]> roadList, List<RoadInfo> roadInfos, List<CrossInfo> crossInfos,
			List<Integer[]> carList) {
		//set this car's cross state("ahead","left","right")
		this.nextRoad = null;
		if (this.carPath.size() >= 3) {
			for (int i = 0; i < roadList.size(); i++) {
				if ((roadList.get(i)[4].equals(this.carPath.get(1)) && roadList.get(i)[5].equals(this.carPath.get(2)))
						|| (roadList.get(i)[5].equals(this.carPath.get(1))
								&& roadList.get(i)[4].equals(this.carPath.get(2)))) {

					this.nextRoad = FindMethod.findRoad(roadInfos, roadList.get(i)[0], carList);
				}
			}
			if (this.nowRoad != null) {
				CrossInfo crossInfo = FindMethod.findCross(crossInfos, this.carTo);
				if (crossInfo.getCrossRoadTop() == this.nowRoad) {
					if (crossInfo.getCrossRoadRight() == nextRoad) {
						this.stateCross = "left";
					} else if (crossInfo.getCrossRoadLeft() == nextRoad) {
						this.stateCross = "right";
					} else if (crossInfo.getCrossRoadBottom() == nextRoad) {
						this.stateCross = "ahead";
					} else {
						System.out.println("cuowu");
					}
				} else if (crossInfo.getCrossRoadRight() == this.nowRoad) {
					if (crossInfo.getCrossRoadTop() == nextRoad) {
						this.stateCross = "right";
					} else if (crossInfo.getCrossRoadBottom() == nextRoad) {
						this.stateCross = "left";
					} else if (crossInfo.getCrossRoadLeft() == nextRoad) {
						this.stateCross = "ahead";
					} else {
						System.out.println("cuowu");
					}
				} else if (crossInfo.getCrossRoadBottom() == this.nowRoad) {
					if (crossInfo.getCrossRoadLeft() == nextRoad) {
						this.stateCross = "left";
					} else if (crossInfo.getCrossRoadRight() == nextRoad) {
						this.stateCross = "right";
					} else if (crossInfo.getCrossRoadTop() == nextRoad) {
						this.stateCross = "ahead";
					} else {
						System.out.println("cuowu");
					}
				} else if (crossInfo.getCrossRoadLeft() == this.nowRoad) {
					if (crossInfo.getCrossRoadTop() == nextRoad) {
						this.stateCross = "left";
					} else if (crossInfo.getCrossRoadBottom() == nextRoad) {
						this.stateCross = "right";
					} else if (crossInfo.getCrossRoadRight() == nextRoad) {
						this.stateCross = "ahead";
					} else {
						System.out.println("cuowu");
					}
				}
			} else if (this.nowRoadTurn != null) {
				CrossInfo crossInfo = FindMethod.findCross(crossInfos, this.carTo);
				if (crossInfo.getCrossRoadTop() == this.nowRoadTurn) {
					if (crossInfo.getCrossRoadRight() == nextRoad) {
						this.stateCross = "left";
					} else if (crossInfo.getCrossRoadLeft() == nextRoad) {
						this.stateCross = "right";
					} else if (crossInfo.getCrossRoadBottom() == nextRoad) {
						this.stateCross = "ahead";
					} else {
						System.out.println("cuowu1");
					}
				} else if (crossInfo.getCrossRoadRight() == this.nowRoadTurn) {
					if (crossInfo.getCrossRoadTop() == nextRoad) {
						this.stateCross = "right";
					} else if (crossInfo.getCrossRoadBottom() == nextRoad) {
						this.stateCross = "left";
					} else if (crossInfo.getCrossRoadLeft() == nextRoad) {
						this.stateCross = "ahead";
					} else {
						System.out.println("cuowu2");
					}
				} else if (crossInfo.getCrossRoadBottom() == this.nowRoadTurn) {
					if (crossInfo.getCrossRoadLeft() == nextRoad) {
						this.stateCross = "left";
					} else if (crossInfo.getCrossRoadRight() == nextRoad) {
						this.stateCross = "right";
					} else if (crossInfo.getCrossRoadTop() == nextRoad) {
						this.stateCross = "ahead";
					} else {
						System.out.println("cuowu3");
					}
				} else if (crossInfo.getCrossRoadLeft() == this.nowRoadTurn) {
					if (crossInfo.getCrossRoadTop() == nextRoad) {
						this.stateCross = "left";
					} else if (crossInfo.getCrossRoadBottom() == nextRoad) {
						this.stateCross = "right";
					} else if (crossInfo.getCrossRoadRight() == nextRoad) {
						this.stateCross = "ahead";
					} else {
						System.out.println("cuowu4");
					}
				}
			}

		} else {
			this.stateCross = "ahead";
		}

	}

	public RoadInfo getNowRoad() {
		return nowRoad;
	}

	public RoadInfo getNowRoadTurn() {
		return nowRoadTurn;
	}

	public void setNowRoad(List<Integer[]> roadList, List<RoadInfo> roadInfos, List<Integer[]> carList) {
		//get this car's position(roadInfo)
		for (int i = 0; i < roadList.size(); i++) {
			if (roadList.get(i)[4].equals(this.carFrom) && roadList.get(i)[5].equals(this.carTo)) {
				this.nowRoad = FindMethod.findRoad(roadInfos, roadList.get(i)[0], carList);
				this.nowRoadTurn = null;
			} else if (roadList.get(i)[6].equals(1) && roadList.get(i)[5].equals(this.carFrom)
					&& roadList.get(i)[4].equals(this.carTo)) {
				this.nowRoad = null;
				this.nowRoadTurn = FindMethod.findRoad(roadInfos, roadList.get(i)[0], carList);
			}
		}
	}

}

class RoadInfo {
	private Integer roadId;
	private Integer roadLength;
	private Integer roadSpeed;
	private Integer roadChannel;
	private Integer roadFrom;
	private Integer roadTo;
	private Integer isDuplex;
	private CarInfo[][] roadArray;
	private CarInfo[][] roadTurnArray;
	private List<CarInfo> roadArraySequence;
	private List<CarInfo> roadTurnArraySequence;
	private List<CarInfo> roadArrayWaitCars;
	private List<CarInfo> roadArrayTurnWaitCars;

	public RoadInfo(Integer roadId, Integer roadLength, Integer roadSpeed, Integer roadChannel, Integer roadFrom,
			Integer roadTo, Integer isDuplex) {
		super();
		this.roadId = roadId;
		this.roadLength = roadLength;
		this.roadSpeed = roadSpeed;
		this.roadChannel = roadChannel;
		this.roadFrom = roadFrom;
		this.roadTo = roadTo;
		this.isDuplex = isDuplex;
		this.roadArray = new CarInfo[this.roadChannel][this.roadLength];
		if (this.isDuplex.equals(1)) {
			this.roadTurnArray = new CarInfo[this.roadChannel][this.roadLength];
		}
		this.roadArrayWaitCars = new ArrayList<CarInfo>();
		this.roadArrayTurnWaitCars = new ArrayList<CarInfo>();
	}

	public List<CarInfo> getRoadArraySequence() {
		return roadArraySequence;
	}

	public void setRoadArraySequence(List<CarInfo> roadArraySequence) {
		this.roadArraySequence = roadArraySequence;
	}

	public List<CarInfo> getRoadTurnArraySequence() {
		return roadTurnArraySequence;
	}

	public void setRoadTurnArraySequence(List<CarInfo> roadTurnArraySequence) {
		this.roadTurnArraySequence = roadTurnArraySequence;
	}

	public void createSequence() {
		// TODO Auto-generated method stub
		CarInfo[][] carInfos = this.roadArray;
		this.roadArraySequence = orderCarPriority(carInfos);
		if (this.isDuplex == 1) {
			carInfos = this.roadTurnArray;
			this.roadTurnArraySequence = orderCarPriority(carInfos);
		}

	}

	private List<CarInfo> orderCarPriority(CarInfo[][] carInfos) {
		List<CarInfo> priorityList = new ArrayList<CarInfo>();
		List<CarInfo> normalList = new ArrayList<CarInfo>();
		for (int i = carInfos[0].length - 1; i >= 0; i--) {
			for (int j = 0; j < carInfos.length; j++) {
				if (carInfos[j][i] != null && carInfos[j][i].getStateCar().equals("wait")) {
					int preCar = 0;
					int nullPosition = 0;
					for (int k = i + 1; k < carInfos[0].length; k++) {
						if (carInfos[j][k] != null) {
							preCar = k;
							break;
						}else {
							nullPosition += 1;
						}
					}
					if (nullPosition < Math.min(carInfos[j][i].getCarSpeed(),this.roadSpeed)  && preCar == 0) {
						if (carInfos[j][i].getCarPriority() == 1) {
							priorityList.add(carInfos[j][i]);
						} else if (carInfos[j][i].getCarPriority() == 0) {
							normalList.add(carInfos[j][i]);
						} else {
							System.out.println("priority  wrong");
						}
					}
					
				}
			}
		}
		if (priorityList.size() == 0) {
			return normalList;
		}else {
			return priorityList;
		}
	}

	public List<CarInfo> getRoadArrayWaitCars() {
		return roadArrayWaitCars;
	}

	public void setRoadArrayWaitCars(List<CarInfo> roadArrayWaitCars) {
		this.roadArrayWaitCars = roadArrayWaitCars;
	}

	public List<CarInfo> getRoadArrayTurnWaitCars() {
		return roadArrayTurnWaitCars;
	}

	public void setRoadArrayTurnWaitCars(List<CarInfo> roadArrayTurnWaitCars) {
		this.roadArrayTurnWaitCars = roadArrayTurnWaitCars;
	}

	public Integer getRoadId() {
		return roadId;
	}

	public void setRoadId(Integer roadId) {
		this.roadId = roadId;
	}

	public Integer getRoadLength() {
		return roadLength;
	}

	public void setRoadLength(Integer roadLength) {
		this.roadLength = roadLength;
	}

	public Integer getRoadSpeed() {
		return roadSpeed;
	}

	public void setRoadSpeed(Integer roadSpeed) {
		this.roadSpeed = roadSpeed;
	}

	public Integer getRoadChannel() {
		return roadChannel;
	}

	public void setRoadChannel(Integer roadChannel) {
		this.roadChannel = roadChannel;
	}

	public Integer getRoadFrom() {
		return roadFrom;
	}

	public void setRoadFrom(Integer roadFrom) {
		this.roadFrom = roadFrom;
	}

	public Integer getRoadTo() {
		return roadTo;
	}

	public void setRoadTo(Integer roadTo) {
		this.roadTo = roadTo;
	}

	public Integer getIsDuplex() {
		return isDuplex;
	}

	public void setIsDuplex(Integer isDuplex) {
		this.isDuplex = isDuplex;
	}

	public CarInfo[][] getRoadArray() {
		return roadArray;
	}

	public void setRoadArray(CarInfo[][] roadArray) {
		this.roadArray = roadArray;
	}

	public CarInfo[][] getRoadTurnArray() {
		return roadTurnArray;
	}

	public void setRoadTurnArray(CarInfo[][] roadTurnArray) {
		this.roadTurnArray = roadTurnArray;
	}
}