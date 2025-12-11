package sim;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Properties;

import person.AbstractIndividualInterface;
import relationship.ContactMap;
import util.LineCollectionEntry;

public class Runnable_MetaPopulation_Transmission_RMP_MultiInfection_POC
		extends Runnable_MetaPopulation_MultiTransmission {

	public static final int SITE_VAGINA = 0;
	public static final int SITE_PENIS = SITE_VAGINA + 1;
	public static final int SITE_ANY = SITE_PENIS + 1; // Not Used
	
	protected static final int[] COL_SEL_INF_GENDER = null;

	protected int lastTestSch_update = -1;

	protected static final int FIELD_TESTING_RATE_COVERAGE = FIELD_TESTING_RATE_BY_RISK_CATEGORIES_TEST_RATE_PARAM_START;

	// Retesting
	protected static final int FIELD_TESTING_RETEST_POS_INF_INCL = FIELD_TESTING_RATE_COVERAGE + 1;
	protected static final int FIELD_TESTING_RETEST_PROB_START = FIELD_TESTING_RETEST_POS_INF_INCL + 1;

	// Key = time
	// V= new Object[] {
	// new int[] {infId, pid_t},
	// (int[][]) inf_stage
	// (int[][]) cumul_treatment_by_person
	protected HashMap<Integer, ArrayList<Object[]>> schedule_treatment = new HashMap<>();

	// Default parameter
	private static final int NUM_INF = 4;
	private static final int NUM_SITE = 3;
	private static final int NUM_ACT = 1;

	public Runnable_MetaPopulation_Transmission_RMP_MultiInfection_POC(long cMap_seed, long sim_seed, Properties prop) {
		super(cMap_seed, sim_seed, prop, NUM_INF, NUM_SITE, NUM_ACT);
	}	
	
	@Override
	protected void handleRemovePerson(Integer pid) {
		// Do nothing
	}

	@Override
	public void scheduleNextTest(Integer personId, int lastTestTime, int mustTestBefore, int last_test_infIncl,
			int last_test_siteIncl) {
		// Do nothing as testing is set up through initialisation and postTimeStep
	}

	@Override
	protected void postTimeStep(int currentTime) {
		super.postTimeStep(currentTime);
		if (lastTestSch_update >= 0
				&& (currentTime + 1) == lastTestSch_update + AbstractIndividualInterface.ONE_YEAR_INT) {
			setAnnualTestingSchdule(currentTime + 1);
			lastTestSch_update = currentTime;
		}
		// Delay treatment
		ArrayList<Object[]> sch_tr = schedule_treatment.remove(currentTime);
		if (sch_tr != null) {
			for (Object[] ent : sch_tr) {
				int[] int_stat = (int[]) ent[0];
				int infId = int_stat[0];
				int pid = int_stat[1];
				int[][] cumul_treatment_by_person = (int[][]) ent[1];
				cumul_treatment_by_person[infId][getPersonGrp(pid)]++;
				applyTreatment(currentTime, infId, pid, map_currrent_infection_stage.get(pid));
			}
		}
		// Movement
		loadMovement(currentTime);

		// Store pop size
		if (currentTime % nUM_TIME_STEPS_PER_SNAP == 0) {
			@SuppressWarnings("unchecked")
			HashMap<Integer, int[]> countMap = (HashMap<Integer, int[]>) sim_output.get(key_pop_size);
			if (countMap == null) {
				countMap = new HashMap<>();
				sim_output.put(key_pop_size, countMap);
			}
			int[] pop_size = new int[NUM_GRP];
			for (int g = 0; g < pop_size.length; g++) {
				pop_size[g] = current_pids_by_grp.get(g).size();
			}
			countMap.put(currentTime, pop_size);
		}

	}

	@SuppressWarnings("unchecked")
	@Override
	protected void postSimulation() {
		super.postSimulation();
		String key, fileName;
		HashMap<Integer, int[]> countMap;
		String filePrefix = this.getRunnableId() == null ? "" : this.getRunnableId();
		PrintWriter pWri;

		key = key_pop_size;

		countMap = (HashMap<Integer, int[]>) sim_output.get(key);
		fileName = String.format(filePrefix + FILENAME_EXPORT_POP_SIZE, cMAP_SEED, sIM_SEED);
		printCountMap(countMap, fileName, "Group_%d", new int[] { NUM_GRP }, null);

		if ((simSetting & 1 << Simulation_ClusterModelTransmission.SIM_SETTING_KEY_GEN_INCIDENCE_FILE) != 0) {

			key = String.format(SIM_OUTPUT_KEY_CUMUL_INCIDENCE,
					Simulation_ClusterModelTransmission.SIM_SETTING_KEY_GEN_INCIDENCE_FILE);
			countMap = (HashMap<Integer, int[]>) sim_output.get(key);
			fileName = String.format(filePrefix + Simulation_ClusterModelTransmission.FILENAME_CUMUL_INCIDENCE_PERSON,
					cMAP_SEED, sIM_SEED);
			printCountMap(countMap, fileName, "Inf_%d_Group_%d", new int[] { NUM_INF, NUM_GRP }, COL_SEL_INF_GENDER);

		}

		if ((simSetting & 1 << Simulation_ClusterModelTransmission.SIM_SETTING_KEY_GEN_PREVAL_FILE) != 0) {

			key = String.format(SIM_OUTPUT_KEY_INFECTIOUS_GENDER_COUNT,
					Simulation_ClusterModelTransmission.SIM_SETTING_KEY_GEN_PREVAL_FILE);
			countMap = (HashMap<Integer, int[]>) sim_output.get(key);
			fileName = String.format(
					filePrefix + "Infectious_" + Simulation_ClusterModelTransmission.FILENAME_PREVALENCE_PERSON,
					cMAP_SEED, sIM_SEED);
			printCountMap(countMap, fileName, "Inf_%d_Gender_%d", new int[] { NUM_INF, NUM_GRP }, COL_SEL_INF_GENDER);

//			key = String.format(SIM_OUTPUT_KEY_INFECTED_AT_GENDER_COUNT,
//					Simulation_ClusterModelTransmission.SIM_SETTING_KEY_GEN_PREVAL_FILE);
//			countMap = (HashMap<Integer, int[]>) sim_output.get(key);
//			fileName = String.format(
//					filePrefix + "Infected_" + Simulation_ClusterModelTransmission.FILENAME_PREVALENCE_SITE, cMAP_SEED,
//					sIM_SEED);
//			printCountMap(countMap, fileName, "Inf_%d_Gender_%d_Infected_SiteInc_%d",
//					new int[] { NUM_INF, NUM_GRP, 1 << (NUM_SITE + 1) }, COL_SEL_INF_GENDER_SITE_AT);

		}
		if ((simSetting & 1 << Simulation_ClusterModelTransmission.SIM_SETTING_KEY_TRACK_INFECTION_HISTORY) > 0) {

			Integer[] pids = infection_history.keySet().toArray(new Integer[infection_history.size()]);
			Arrays.sort(pids);
			try {
				pWri = new PrintWriter(new File(baseDir,
						String.format(filePrefix + Simulation_ClusterModelTransmission.FILENAME_INFECTION_HISTORY,
								cMAP_SEED, sIM_SEED)));
				for (Integer pid : pids) {
					ArrayList<ArrayList<Integer>> hist = infection_history.get(pid);
					for (int infId = 0; infId < hist.size(); infId++) {
						pWri.print(pid.toString());
						pWri.print(',');
						pWri.print(infId);
						for (Integer timeEnt : hist.get(infId)) {
							pWri.print(',');
							pWri.print(timeEnt);
						}
						pWri.println();
					}
				}

				pWri.close();
			} catch (IOException e) {
				e.printStackTrace(System.err);
			}
		}

		for (LineCollectionEntry movementEntry : movementCollections.values()) {
			try {
				movementEntry.closeReader();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

	@Override
	protected int initaliseCMap(ContactMap cMap, Integer[][] edges_array, int edges_array_pt, int startTime,
			HashMap<Integer, ArrayList<Integer[]>> removeEdges) {
		int res = super.initaliseCMap(cMap, edges_array, edges_array_pt, startTime, removeEdges);
		lastTestSch_update = startTime;
		setAnnualTestingSchdule(startTime);		
		return res;
	}

	@Override
	protected void testPerson(int currentTime, int pid_t, int infIncl, int siteIncl,
			int[][] cumul_treatment_by_person) {
		ArrayList<Integer> tested_positive = new ArrayList<>();

		if (pid_t < 0) { // Assume test and treat as normal with symptoms
			int[] preTreatCount = new int[NUM_INF];
			for (int infId = 0; infId < NUM_INF; infId++) {
				preTreatCount[infId] = cumul_treatment_by_person[infId][getPersonGrp(-pid_t)];
			}
			super.testPerson(currentTime, pid_t, infIncl, siteIncl, cumul_treatment_by_person);

			for (int infId = 0; infId < NUM_INF; infId++) {
				if (preTreatCount[infId] != cumul_treatment_by_person[infId][getPersonGrp(-pid_t)]) {
					tested_positive.add(infId);
				}
			}
		} else {
			double[][] testRateDefs = (double[][]) getRunnable_fields()[RUNNABLE_FIELD_TRANSMISSION_TESTING_RATE_BY_RISK_CATEGORIES];
			// Check which testRateDef fit
			double[] testRateDefMatch = null;
			int pid = pid_t;
			for (double[] testRateDef : testRateDefs) {
				if (testRateDef[FIELD_TESTING_RATE_COVERAGE] >= 0) {
					int gIncl = (int) testRateDef[FIELD_TESTING_RATE_BY_RISK_CATEGORIES_GENDER_INCLUDE_INDEX];
					int sIncl = (int) testRateDef[FIELD_TESTING_RATE_BY_RISK_CATEGORIES_SITE_INCLUDE_INDEX];
					int iIncl = (int) testRateDef[FIELD_TESTING_RATE_BY_RISK_CATEGORIES_INF_INCLUDE_INDEX];
					if ((1 << getPersonGrp(pid) & gIncl) != 0 && (sIncl & siteIncl) != 0 && (iIncl & infIncl) != 0) {
						testRateDefMatch = testRateDef;
					}
				}
			}
			if (testRateDefMatch == null) {
				System.err.printf(
						"Warning! Test defintion for [t=%d,pid=%d,infIncl=%d,siteIncl=%d] NOT found. Use default test person instead.\n",
						currentTime, pid, infIncl, siteIncl);
				int[] preTreatCount = new int[NUM_INF];
				for (int infId = 0; infId < NUM_INF; infId++) {
					preTreatCount[infId] = cumul_treatment_by_person[infId][getPersonGrp(pid_t)];
				}
				super.testPerson(currentTime, pid, infIncl, siteIncl, cumul_treatment_by_person);
				for (int infId = 0; infId < NUM_INF; infId++) {
					if (preTreatCount[infId] != cumul_treatment_by_person[infId][getPersonGrp(pid_t)]) {
						tested_positive.add(infId);
					}
				}
			} else {
				int[][] inf_stage = map_currrent_infection_stage.get(pid);

				final int offset_num_delay_option = 1;
				final int offset_num_delay_range = offset_num_delay_option + 1;

				ArrayList<Integer> delayRangeForInf = new ArrayList<>();
				ArrayList<Integer> applyTreatmentForInf = new ArrayList<>();

				if (inf_stage != null) {
					int num_delay_option;
					int num_delay_range;

					// Find delay range as defined by infection type
					int delay_inf_incl_pt = FIELD_TESTING_RATE_COVERAGE + 1;

					while (delay_inf_incl_pt < testRateDefMatch.length) {
						num_delay_option = (int) testRateDefMatch[delay_inf_incl_pt + offset_num_delay_option];
						num_delay_range = (int) testRateDefMatch[delay_inf_incl_pt + offset_num_delay_range];

						delayRangeForInf.clear();
						applyTreatmentForInf.clear();

						for (int infId = 0; infId < NUM_INF; infId++) {
							if ((((int) testRateDefMatch[delay_inf_incl_pt]) & 1 << infId) != 0) {
								delayRangeForInf.add(infId);
							}
						} // End of for (int infId = 0; infId < NUM_INF; infId++) {

						if (!delayRangeForInf.isEmpty()) {

							double[] test_properties;

							// Check if tested positive
							for (Integer infId : delayRangeForInf) {
								for (int siteId = 0; siteId < NUM_SITE; siteId++) {
									// Test for the site
									test_properties = lookupTable_test_treatment_properties
											.get(String.format("%d,%d", infId, siteId));
									if (test_properties != null) {
										if (inf_stage[infId][siteId] >= 0) { // Infected
											double testSensitivity = 0;
											int stage_pt = FIELD_DX_TEST_PROPERTIES_ACCURACY_START;
											while (testSensitivity == 0 && stage_pt < test_properties.length) {
												// TEST_ACCURACY_1, TARGET_STAGE_INC_1, TREATMENT_SUC_STAGE_1 ..
												int tested_stage_inc = (int) test_properties[stage_pt + 1];
												if ((tested_stage_inc & 1 << inf_stage[infId][siteId]) != 0) {
													testSensitivity = test_properties[stage_pt];
												}
												stage_pt += 3;
											}
											if (testSensitivity > 0 && RNG.nextDouble() < testSensitivity) {
												applyTreatmentForInf.add(infId);
											}
										}
									}
								}
							} // End of for (Integer infId : inf_in_delay_range) {

							if (!applyTreatmentForInf.isEmpty()) {

								// Determine treatment delay
								double prob;
								int pt;
								prob = RNG.nextDouble();
								int delay_option_start_pt = delay_inf_incl_pt + offset_num_delay_range + 1;
								pt = Arrays.binarySearch(testRateDefMatch, delay_option_start_pt,
										delay_option_start_pt + num_delay_option - 1, prob);
								if (pt < 0) {
									pt = ~pt;
								}
								int delay_prob_start_pt = 1 + delay_inf_incl_pt + offset_num_delay_range
										+ num_delay_option + (pt - delay_option_start_pt) * 2 * num_delay_range;

								prob = RNG.nextDouble();
								pt = Arrays.binarySearch(testRateDefMatch, delay_prob_start_pt,
										delay_prob_start_pt + num_delay_range - 1, prob);

								if (pt < 0) {
									pt = ~pt;
								}
								if ((pt - delay_prob_start_pt + 1) < num_delay_range) { // Miss out on treatment
																						// otherwise
									int delay = (int) testRateDefMatch[pt + num_delay_range];
									delay += RNG.nextInt((int) testRateDefMatch[pt + num_delay_range + 1] - delay);

									for (int infId : applyTreatmentForInf) {
										tested_positive.add(infId);
										if (delay <= 1) {
											cumul_treatment_by_person[infId][getPersonGrp(pid)]++;
											applyTreatment(currentTime, infId, pid, inf_stage);
										} else {
											ArrayList<Object[]> sch_treat = schedule_treatment.get(currentTime + delay);
											if (sch_treat == null) {
												sch_treat = new ArrayList<>();
												schedule_treatment.put(currentTime + delay, sch_treat);
											}
											sch_treat.add(new Object[] { new int[] { infId, pid },
													cumul_treatment_by_person });
										}
									}
								}
							} // End of if (applyTreatmentForInf) {
						} // End of if (!delayRangeForInf.isEmpty()) {

						// Proceed to test the next delay option
						delay_inf_incl_pt += offset_num_delay_range + num_delay_option
								+ num_delay_option * 2 * num_delay_range + 1;

					} // End of while (delay_inf_incl_pt < testRateDefMatch.length) {
				} // End of if(inf_stage != null) {
			} // End of if (testRateDefMatch == null) {... } else {...
		} // End of if (pid_t < 0) {...} else {

		// Retesting

		double[][] testRateDefs = (double[][]) getRunnable_fields()[RUNNABLE_FIELD_TRANSMISSION_TESTING_RATE_BY_RISK_CATEGORIES];
		int pid = Math.abs(pid_t);
		for (double[] testRateDef : testRateDefs) {
			int gIncl = (int) testRateDef[FIELD_TESTING_RATE_BY_RISK_CATEGORIES_GENDER_INCLUDE_INDEX];

			if ((1 << getPersonGrp(pid) & gIncl) != 0) {
				if (testRateDef[FIELD_TESTING_RATE_COVERAGE] < 0) {
					boolean match = false;
					int inf_incl = (int) testRateDef[FIELD_TESTING_RETEST_POS_INF_INCL];
					if (inf_incl == AbstractIndividualInterface.INFECT_S) {
						match = tested_positive.isEmpty();
					} else {
						for (int infId : tested_positive) {
							match |= (inf_incl & 1 << infId) != 0;
						}
					}
					if (match) {
						double[] retestDefMatch = testRateDef;
						double pRetest = RNG.nextDouble(); // Retest Range
						int retest_range_offset = (retestDefMatch.length - FIELD_TESTING_RETEST_PROB_START) / 2;
						int pt_pRestest = Arrays.binarySearch(retestDefMatch, FIELD_TESTING_RETEST_PROB_START,
								FIELD_TESTING_RETEST_PROB_START + retest_range_offset - 1, pRetest);

						if (pt_pRestest < 0) {
							pt_pRestest = ~pt_pRestest;
						}
						pt_pRestest = pt_pRestest + retest_range_offset;

						if (pt_pRestest + 1 < retestDefMatch.length) {
							int retest_time = currentTime + (int) retestDefMatch[pt_pRestest] + RNG
									.nextInt((int) retestDefMatch[pt_pRestest + 1] - (int) retestDefMatch[pt_pRestest]);

							ArrayList<int[]> sch_test = schedule_testing.get(retest_time);
							if (sch_test == null) {
								sch_test = new ArrayList<>();
								schedule_testing.put(retest_time, sch_test);
							}
							int[] test_pair = new int[] { pid,
									(int) retestDefMatch[FIELD_TESTING_RATE_BY_RISK_CATEGORIES_INF_INCLUDE_INDEX],
									(int) retestDefMatch[FIELD_TESTING_RATE_BY_RISK_CATEGORIES_SITE_INCLUDE_INDEX] };
							int pt_t = Collections.binarySearch(sch_test, test_pair, new Comparator<int[]>() {
								@Override
								public int compare(int[] o1, int[] o2) {
									int res = 0;
									int pt = 0;
									while (res == 0 && pt < o1.length) {
										res = Integer.compare(o1[pt], o2[pt]);
										pt++;
									}
									return res;
								}
							});
							if (pt_t < 0) {
								sch_test.add(~pt_t, test_pair);
							} else {
								int[] org_pair = sch_test.get(pt_t);
								org_pair[1] |= infIncl;
							}
						}
					}

				}
			}
		}

	}

	@Override
	protected void applyTreatment(int currentTime, int infId, int pid, int[][] inf_stage) {

		int[] preTreatment_stage = Arrays.copyOf(inf_stage[infId], inf_stage[infId].length);

		super.applyTreatment(currentTime, infId, pid, inf_stage);

		if ((simSetting & 1 << Simulation_ClusterModelTransmission.SIM_SETTING_KEY_TRACK_INFECTION_HISTORY) > 0) {
			ArrayList<Integer> infHist = infection_history.get(pid).get(infId);

			boolean nonInfected = true;
			boolean treatment_suc = false;

			for (int i = 0; i < preTreatment_stage.length; i++) {
				nonInfected &= preTreatment_stage[i] == AbstractIndividualInterface.INFECT_S;
				treatment_suc |= preTreatment_stage[i] >= 0 && preTreatment_stage[i] != inf_stage[infId][i];
			}
			if (nonInfected) {
				infHist.add(currentTime);
				infHist.add(currentTime);
				infHist.add(INFECTION_HIST_OVERTREATMENT);
			} else if (treatment_suc) {
				if (infHist.get(infHist.size() - 1) > 0) {
					infHist.add(currentTime);
					infHist.add(INFECTION_HIST_CLEAR_TREATMENT);
				} else {
					System.err.printf("Infection history error: %s -> %s.\n", Arrays.toString(preTreatment_stage),
							Arrays.toString(inf_stage[infId]));

				}
			}
		}

	}

	@Override
	public int addInfectious(Integer infectedPId, int infectionId, int site_id, int stage_id, int infectious_time,
			int state_duration_adj) {
		int res = super.addInfectious(infectedPId, infectionId, site_id, stage_id, infectious_time, state_duration_adj);

		if ((simSetting & 1 << Simulation_ClusterModelTransmission.SIM_SETTING_KEY_TRACK_INFECTION_HISTORY) > 0) {
			ArrayList<ArrayList<Integer>> hist_all = infection_history.get(infectedPId);
			if (hist_all == null) {
				hist_all = new ArrayList<>();
				for (int i = 0; i < NUM_INF; i++) {
					hist_all.add(new ArrayList<>());
				}
				infection_history.put(infectedPId, hist_all);
			}
			ArrayList<Integer> hist_by_inf = hist_all.get(infectionId);
			// Check for new infection (i.e. previously recovered naturally or through
			// treatment
			if (hist_by_inf.size() == 0 || hist_by_inf.get(hist_by_inf.size() - 1) < 0) {
				hist_by_inf.add(infectious_time);
			}
		}
		return res;
	}

	@Override
	protected int[] handleNoNextStage(Integer pid, int infection_id, int site_id, int current_infection_stage,
			int current_time) {
		int[] res = super.handleNoNextStage(pid, infection_id, site_id, current_infection_stage, current_time);
		// res = {next_stage, duration}
		if ((simSetting & 1 << Simulation_ClusterModelTransmission.SIM_SETTING_KEY_TRACK_INFECTION_HISTORY) > 0) {
			ArrayList<Integer> infhist = infection_history.get(pid).get(infection_id);
			if (infhist.size() > 0 && infhist.get(infhist.size() - 1) > 0) {
				// Key=PID,V=int[INF_ID][SITE]{infection_stage}
				int[] inf_stat = map_currrent_infection_stage.get(pid)[infection_id];
				boolean all_clear = true;
				for (int s = 0; s < inf_stat.length; s++) {
					all_clear &= (s == site_id ? res[0] : inf_stat[s]) == AbstractIndividualInterface.INFECT_S;
				}
				if (all_clear) {
					ArrayList<Integer> infHist = infection_history.get(pid).get(infection_id);
					infHist.add(current_time);
					infHist.add(INFECTION_HIST_CLEAR_NATURAL_RECOVERY);
				}
			}

		}

		return res;
	}

	@Override
	protected double getTransmissionProb(int currentTime, int inf_id, int pid_inf_src, int pid_inf_tar,
			int partnershiptDur, int actType, int src_site, int tar_site) {
		if (indiv_map.get(pid_inf_src)[INDIV_MAP_CURRENT_LOC] != indiv_map.get(pid_inf_tar)[INDIV_MAP_CURRENT_LOC]) {
			// Only possible at same location
			return 0;
		} else {
			return super.getTransmissionProb(currentTime, inf_id, pid_inf_src, pid_inf_tar, partnershiptDur, actType,
					src_site, tar_site);
		}
	}

	protected void loadMovement(int movementUpToTime) {
		while (lastMovement_update <= movementUpToTime) {
			for (Entry<String, LineCollectionEntry> mvE : movementCollections.entrySet()) {
				String[] direction = mvE.getKey().split("_");
				while ((mvE.getValue().getCurrentLine()) != null) {
					String[] ent = mvE.getValue().getCurrentLine().split(",");
					if (Integer.parseInt(ent[0]) == lastMovement_update) {
						int pid = Integer.parseInt(ent[1]);
						int[] indiv_stat = indiv_map.get(pid);
						int src_loc = Integer.parseInt(direction[0]);
						int tar_loc = Integer.parseInt(direction[1]);
						indiv_stat[INDIV_MAP_CURRENT_LOC] = tar_loc;
						if (tar_loc != indiv_stat[INDIV_MAP_HOME_LOC]) {
							// Moving away from home
							ArrayList<Integer> loc_arr = visitor_pids_by_loc.get(tar_loc);
							if (loc_arr == null) {
								loc_arr = new ArrayList<>();
								visitor_pids_by_loc.put(tar_loc, loc_arr);
							}
							loc_arr.add(~Collections.binarySearch(loc_arr, pid), pid);
						} else {
							// Returning home
							ArrayList<Integer> loc_arr = visitor_pids_by_loc.get(src_loc);
							loc_arr.remove(Collections.binarySearch(loc_arr, pid));
						}
						mvE.getValue().loadNextLine();
					} else {
						break;
					}
				}
			}
			lastMovement_update++;
		}
	}

	protected void setAnnualTestingSchdule(int testStartTime) {
		double[][] testRateDefs = (double[][]) getRunnable_fields()[RUNNABLE_FIELD_TRANSMISSION_TESTING_RATE_BY_RISK_CATEGORIES];
		for (double[] testRateDef : testRateDefs) {
			if (testRateDef[FIELD_TESTING_RATE_COVERAGE] > 0) {
				int gIncl = (int) testRateDef[FIELD_TESTING_RATE_BY_RISK_CATEGORIES_GENDER_INCLUDE_INDEX];
				int num_test_candidate_per_def = 0;
				// Risk group not used
				// int rIncl = (int)
				// testRateDef[FIELD_TESTING_RATE_BY_RISK_CATEGORIES_RISK_GRP_INCLUDE_INDEX];
				int sIncl = (int) testRateDef[FIELD_TESTING_RATE_BY_RISK_CATEGORIES_SITE_INCLUDE_INDEX];
				int iIncl = (int) testRateDef[FIELD_TESTING_RATE_BY_RISK_CATEGORIES_INF_INCLUDE_INDEX];
				for (int g = 0; g < NUM_GRP; g++) {
					if ((gIncl & 1 << g) != 0) {
						num_test_candidate_per_def += current_pids_by_grp.get(g).size();
					}
				}
				int num_tests_peformed_per_def = (int) Math
						.round(num_test_candidate_per_def * testRateDef[FIELD_TESTING_RATE_COVERAGE]);
				int person_index = 0;
				for (int g = 0; g < NUM_GRP; g++) {
					if ((gIncl & 1 << g) != 0) {
						for (Integer pid : current_pids_by_grp.get(g)) {
							if (RNG.nextInt(num_test_candidate_per_def - person_index) < num_tests_peformed_per_def) {
								int testDate = testStartTime + RNG.nextInt(AbstractIndividualInterface.ONE_YEAR_INT);
								if (testDate < exitPopAt(pid)) {
									ArrayList<int[]> day_sch = schedule_testing.get(testDate);
									if (day_sch == null) {
										day_sch = new ArrayList<>();
										schedule_testing.put(testDate, day_sch);
									}
									int[] test_entry = new int[] { pid, iIncl, sIncl };
									int pt = Collections.binarySearch(day_sch, test_entry, new Comparator<int[]>() {
										@Override
										public int compare(int[] o1, int[] o2) {
											int res = 0;
											int pt_arr = 0;
											while (res == 0 && pt_arr < 3) {
												res = Integer.compare(o1[pt_arr], o2[pt_arr]);
												pt_arr++;
											}
											return res;
										}
									});
									if (pt < 0) {
										day_sch.add(~pt, test_entry);
									}
									num_tests_peformed_per_def--;
								}
							}
							person_index++;
						}
					}
				}

			}
		}
	}

}
