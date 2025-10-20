package util;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import sim.Abstract_Runnable_ClusterModel;
import sim.Simulation_ClusterModelTransmission;

public class Util_Analyse_RMP {

	public static void extract_num_infection_to_csv(File scenario_dirs_incl, int[][] colIndex)
			throws IOException, FileNotFoundException {
		extract_num_infection_to_csv(new File[] { scenario_dirs_incl }, scenario_dirs_incl, colIndex);
	}

	public static void extract_num_infection_to_csv(File[] scenario_dirs_incl, File output_dir, int[][] colIndex)
			throws IOException, FileNotFoundException {
		Comparator<File> cmp_file_suffix = generate_file_comparator_by_suffix();

		ArrayList<ArrayList<StringBuilder>> lines_all_inf = new ArrayList<>();
		for (int p = 0; p < colIndex.length; p++) {
			lines_all_inf.add(new ArrayList<>());
		}

		for (File resultSetDir : scenario_dirs_incl) {
			File[] singleResultSets = resultSetDir.listFiles(new FileFilter() {
				@Override
				public boolean accept(File pathname) {
					return pathname.isDirectory() && !"PBS".equals(pathname.getName());
				}
			});

			Arrays.sort(singleResultSets, cmp_file_suffix);

			Pattern pattern_num_inf_src = Pattern.compile("Infectious_Prevalence_Person_(-?\\d+).csv.7z");

			boolean completedSet = true;
			for (File singleResultSet : singleResultSets) {
				File[] zips;
				zips = singleResultSet.listFiles(new FileFilter() {
					@Override
					public boolean accept(File pathname) {
						return pattern_num_inf_src.matcher(pathname.getName()).matches();
					}
				});

				if (zips.length != 1) {
					System.err.printf("Error. Number of zip in %s != 1\n", singleResultSet.getName());
					// System.out.printf("qsub %s.pbs\n", singleResultSet.getName());
					completedSet = false;
				}
			}

			if (!completedSet) {
				System.exit(-1);
			}

			for (File singleResultSet : singleResultSets) {

				// System.out.printf("Current Result Set: %s\n",
				// singleResultSet.getAbsolutePath());

				File[] zips;

				zips = singleResultSet.listFiles(new FileFilter() {
					@Override
					public boolean accept(File pathname) {
						return pattern_num_inf_src.matcher(pathname.getName()).matches();
					}
				});

				if (zips.length != 1) {
					System.err.printf("Error. Number of zip in %s != 1\n", singleResultSet.getAbsolutePath());
				}

				// Should have one map only
				HashMap<String, ArrayList<String[]>> linesMap = util.Util_7Z_CSV_Entry_Extract_Callable
						.extractedLinesFrom7Zip(zips[0]);
				Pattern pattern_inf_preval_header = Pattern
						.compile("\\[(.+),(\\d+)\\]Infectious_Prevalence_Person_(-?\\d+)_(-?\\d+).csv");

				String[] keys = linesMap.keySet().toArray(new String[0]);
				Arrays.sort(keys, new Comparator<String>() {
					@Override
					public int compare(String o1, String o2) {
						Matcher m1 = pattern_inf_preval_header.matcher(o1);
						Matcher m2 = pattern_inf_preval_header.matcher(o2);
						if (m1.matches() && m2.matches()) {
							int res = Integer.compare(Integer.parseInt(m1.group(2)), Integer.parseInt(m2.group(2)));
							if (res == 0) {
								res = Long.compare(Long.parseLong(m1.group(2)), Long.parseLong(m2.group(2)));
							}
							return res;

						} else {
							return o1.compareTo(o2);
						}
					}
				});

				for (String key : keys) {
					ArrayList<String[]> lines_from_sim = linesMap.get(key);
					for (int p = 0; p < colIndex.length; p++) {
						ArrayList<StringBuilder> lines_inf = lines_all_inf.get(p);
						for (int lineNum = 0; lineNum < lines_from_sim.size(); lineNum++) {
							String[] line_ent = lines_from_sim.get(lineNum);
							while (lineNum >= lines_inf.size()) {
								lines_inf.add(new StringBuilder());
							}
							StringBuilder strBuild = lines_inf.get(lineNum);
							if (strBuild.length() == 0) {
								strBuild.append(line_ent[0]);
							}
							strBuild.append(',');
							if (lineNum == 0) {
								Matcher m = pattern_inf_preval_header.matcher(key);
								if (m.find()) {
									strBuild.append(resultSetDir.getName());
									strBuild.append(':');
									strBuild.append(singleResultSet.getName());
									strBuild.append('(');
									strBuild.append(m.group(2));
									strBuild.append('_');
									strBuild.append(m.group(3));
									strBuild.append('_');
									strBuild.append(m.group(4));
									strBuild.append(')');
								} else {
									strBuild.append(key);
								}
							} else {
								int numInf = 0;
								for (int col : colIndex[p]) {
									numInf += Integer.parseInt(line_ent[col]);
								}
								strBuild.append(numInf);
							}

						}
					}
				}

			}

			PrintWriter[] pWriters_num_infect = new PrintWriter[colIndex.length];
			for (int p = 0; p < colIndex.length; p++) {
				pWriters_num_infect[p] = new PrintWriter(
						new File(output_dir, String.format("Inf_%d_num_of_infected.csv", p)));
				for (StringBuilder lines : lines_all_inf.get(p)) {
					pWriters_num_infect[p].println(lines.toString());
				}
				pWriters_num_infect[p].close();
			}

		}
	}

	public static Comparator<File> generate_file_comparator_by_suffix() {
		Pattern pattern_suffix = Pattern.compile(".*_(\\d+)");
		Comparator<File> cmp_file_suffix = new Comparator<File>() {
			@Override
			public int compare(File o1, File o2) {
				Matcher m1 = pattern_suffix.matcher(o1.getName());
				Matcher m2 = pattern_suffix.matcher(o2.getName());
				m1.find();
				m2.find();
				return Integer.compare(Integer.parseInt(m1.group(1)), Integer.parseInt(m2.group(1)));
			}
		};
		return cmp_file_suffix;
	}

	public static void extract_infection_history_to_csv(File scenario_dir, HashMap<Integer, String[]> indiv_map_by_cmap,
			int switchTime, int[] inf_modelled) throws IOException, FileNotFoundException {

		Pattern pattern_suffix = Pattern.compile(".*_(\\d+)");
		Comparator<File> cmp_file_suffix = new Comparator<File>() {
			@Override
			public int compare(File o1, File o2) {
				Matcher m1 = pattern_suffix.matcher(o1.getName());
				Matcher m2 = pattern_suffix.matcher(o2.getName());
				m1.find();
				m2.find();
				return Integer.compare(Integer.parseInt(m1.group(1)), Integer.parseInt(m2.group(1)));
			}
		};

		ArrayList<ArrayList<StringBuilder>> lines_all_inf = new ArrayList<>();
		for (int p = 0; p < inf_modelled.length; p++) {
			lines_all_inf.add(new ArrayList<>());
		}

		File[] resultSetDirs = scenario_dir.listFiles(new FileFilter() {
			@Override
			public boolean accept(File pathname) {
				return pathname.isDirectory() && pathname.getName().startsWith(scenario_dir.getName());
			}
		});
		Arrays.sort(resultSetDirs, cmp_file_suffix);

		for (File resultSetDir : resultSetDirs) {

			PrintWriter[] pWriters = new PrintWriter[inf_modelled.length];

			Pattern pattern_inf_hist_src = Pattern.compile("InfectHist_(-?\\d+).csv.7z");
			for (int p = 0; p < pWriters.length; p++) {
				pWriters[p] = new PrintWriter(
						new File(resultSetDir, String.format("Inf_%d_duration.csv", inf_modelled[p])));
				pWriters[p].println(String.format("Infection_duration_from_%d,Recovery_Reason,Start_Grp", switchTime));
			}

			File[] zips = resultSetDir.listFiles(new FileFilter() {
				@Override
				public boolean accept(File pathname) {
					return pattern_inf_hist_src.matcher(pathname.getName()).matches();
				}
			});

			for (File zipFile : zips) {

				HashMap<String, ArrayList<String[]>> res = util.Util_7Z_CSV_Entry_Extract_Callable
						.extractedLinesFrom7Zip(zipFile);
				for (ArrayList<String[]> lines : res.values()) {
					for (String[] lineEnt : lines) {
						// pid,inf_id,infection_start_time_1, infection_clear_time_1,
						// infection_clear_reason...
						int col = Arrays.binarySearch(inf_modelled, Integer.parseInt(lineEnt[1]));
						if (col >= 0) {
							for (int start = 2; start < lineEnt.length; start += 3) {
								if (start + 2 < lineEnt.length) {
									int inf_start = Integer.parseInt(lineEnt[start]);
									int inf_finished = Integer.parseInt(lineEnt[start + 1]);
									int dur = inf_finished - inf_start;
									if (inf_finished >= switchTime) {
										pWriters[col].printf("%d,%d,%s\n", dur, Integer.parseInt(lineEnt[start + 2]),
												indiv_map_by_cmap.get(Integer.parseInt(lineEnt[0]))[1]);
									}
								}
							}
						}
					}
				}

			}
		}

	}

	public static HashMap<Long, HashMap<Integer, String[]>> generate_demographic_mapping_from_file(File demograhicDir)
			throws FileNotFoundException, IOException {
		HashMap<Long, HashMap<Integer, String[]>> map_indiv_map = new HashMap<>();
		Pattern pattern_demograhic = Pattern.compile("POP_STAT_(-?\\d+).csv");
		File[] demographicFiles = demograhicDir.listFiles(new FileFilter() {
			@Override
			public boolean accept(File pathname) {
				return pattern_demograhic.matcher(pathname.getName()).matches();
			}
		});
		for (File file_demo : demographicFiles) {
			Matcher m = pattern_demograhic.matcher(file_demo.getName());
			m.find();
			long key = Long.parseLong(m.group(1));
			String[] demographic = util.Util_7Z_CSV_Entry_Extract_Callable.extracted_lines_from_text(file_demo);

			HashMap<Integer, String[]> indiv_map = new HashMap<>();
			for (int i = 1; i < demographic.length; i++) {
				String line = demographic[i];
				String[] sp = line.split(",");
				indiv_map.put(Integer.parseInt(sp[0]), sp);
			}
			map_indiv_map.put(key, indiv_map);
		}
		return map_indiv_map;
	}

	public final static String SETTING_GRP_INCL = "SETTING_GRP_INCL"; // Integer
	public final static String SETTING_BIRTH_RATE = "SETTING_BIRTH_RATE"; // double[]
	public final static String SETTING_BIRTH_BASED_SUBOUTCOMES = "SETTING_BIRTH_BASED_SUBOUTCOMES"; // String[]
	public final static String SETTING_BIRTH_BASED_SUBOUTCOMES_SETTING = "SETTING_BIRTH_BASED_SUBOUTCOMES_SETTING"; // double[][]

	// morbidity_setting_all = Number[] {grp_incl}
	public static void extracted_morbidity_from_InfectHist(File[] sce_dir, File output_dir,
			HashMap<Long, HashMap<Integer, String[]>> demographic,
			Map<String, Map<Integer, double[]>> morbidity_prob_map_all,
			Map<String, Map<String, Object>> morbidity_setting_all,

			int[] sample_time) throws IOException, FileNotFoundException {
		Pattern pattern_inf_hist_zip = Pattern.compile(
				Simulation_ClusterModelTransmission.FILENAME_INFECTION_HISTORY_ZIP.replaceAll("%d", "(-?\\\\d+)"));
		Pattern pattern_inf_preval_header = Pattern.compile(String.format("\\[(.+),(\\d+)\\]%s",
				Simulation_ClusterModelTransmission.FILENAME_INFECTION_HISTORY.replaceAll("%d", "(-?\\\\d+)")));

		final int GRP_INDEX_INF_HISTORY_HEADER_ROW_INDEX = 2;
		final int GRP_INDEX_INF_HISTORY_HEADER_CMAP_SEED = 3;
		final int GRP_INDEX_INF_HISTORY_HEADER_SIM_SEED = 4;

		final int SWITCH_REASON_SAMPLE = 0;
		final int SWITCH_REASON_AGING = SWITCH_REASON_SAMPLE + 1;
		final int SWITCH_REASON_END_PRIMARY_MORBIDITY = SWITCH_REASON_AGING + 1;

		// Key
		// Lv 0: (String) morbid_key
		// Lv 1: (Integer) inf_id
		// Lv 2: (String) sim_key
		// Lv 3: (Integer) sampleTime
		// Value: [Daily_Event_Probabiliy,From_Time,To_Time]

		HashMap<String, HashMap<Integer, HashMap<String, HashMap<Integer, ArrayList<Number[]>>>>> area_under_curve_all = new HashMap<>();
		HashMap<Integer, HashMap<String, HashMap<Integer, ArrayList<Number[]>>>> area_under_curve_by_morbidity;
		HashMap<String, HashMap<Integer, ArrayList<Number[]>>> area_under_curve_by_morbidity_inf;
		HashMap<Integer, ArrayList<Number[]>> area_under_curve_by_morbidity_inf_sim;
		ArrayList<Number[]> area_under_curve_by_morbidity_inf_sim_sampleTime;

		Map<Integer, double[]> morbidity_prob_map;

		// Initialise area_under_curve_by_inf for all infection
		for (String morbid_key : morbidity_prob_map_all.keySet()) {
			morbidity_prob_map = morbidity_prob_map_all.get(morbid_key);
			area_under_curve_by_morbidity = new HashMap<>();
			for (int inf : morbidity_prob_map.keySet()) {
				area_under_curve_by_morbidity_inf = new HashMap<>();
				area_under_curve_by_morbidity.put(inf, area_under_curve_by_morbidity_inf);
			}
			area_under_curve_all.put(morbid_key, area_under_curve_by_morbidity);
		}

		for (File resultSetDir : sce_dir) {
			File[] singleResultSets = resultSetDir.listFiles(new FileFilter() {
				@Override
				public boolean accept(File pathname) {
					return pathname.isDirectory() && !"PBS".equals(pathname.getName());
				}
			});

			for (File singleResultSet : singleResultSets) {
				long tic = System.currentTimeMillis();

				File[] inf_hist_zips = singleResultSet.listFiles(new FileFilter() {
					@Override
					public boolean accept(File pathname) {
						return pattern_inf_hist_zip.matcher(pathname.getName()).matches();
					}
				});

				if (inf_hist_zips.length != 1) {
					System.err.printf("Error. Number of zip in %s != 1\n", singleResultSet.getAbsolutePath());
				} else {
					// Should have one map only
					HashMap<String, ArrayList<String[]>> linesMap = util.Util_7Z_CSV_Entry_Extract_Callable
							.extractedLinesFrom7Zip(inf_hist_zips[0]);

					Matcher m_zip = pattern_inf_hist_zip.matcher(inf_hist_zips[0].getName());
					m_zip.matches();

					long cmap_seed = Long.parseLong(m_zip.group(1));

					HashMap<Integer, String[]> lookup_demograhic = demographic.get(cmap_seed);

					for (Entry<String, ArrayList<String[]>> ent : linesMap.entrySet()) {
						Matcher m = pattern_inf_preval_header.matcher(ent.getKey());
						String simKey;
						if (m.find()) {
							simKey = String.format("%s:%s(%s_%s_%s)", resultSetDir.getName(), singleResultSet.getName(),
									m.group(GRP_INDEX_INF_HISTORY_HEADER_ROW_INDEX),
									m.group(GRP_INDEX_INF_HISTORY_HEADER_CMAP_SEED),
									m.group(GRP_INDEX_INF_HISTORY_HEADER_SIM_SEED));
						} else {
							simKey = String.format("%s:%s_(%s)", resultSetDir.getName(), singleResultSet.getName(),
									ent.getKey());
						}

						for (String[] line : ent.getValue()) {
							int person_id = Integer.parseInt(line[0]);
							String[] indiv_stat = lookup_demograhic.get(person_id);
							int grp = Integer.parseInt(indiv_stat[Abstract_Runnable_ClusterModel.POP_INDEX_GRP + 1]);

							int enter_age = Integer
									.parseInt(indiv_stat[Abstract_Runnable_ClusterModel.POP_INDEX_ENTER_POP_AGE + 1]);
							int enter_at = Integer
									.parseInt(indiv_stat[Abstract_Runnable_ClusterModel.POP_INDEX_ENTER_POP_AT + 1]);

							int exit_at = Integer
									.parseInt(indiv_stat[Abstract_Runnable_ClusterModel.POP_INDEX_EXIT_POP_AT + 1]);

							// Only include individual within sample time window
							if (sample_time[0] <= exit_at && enter_at <= sample_time[sample_time.length - 1]) {
								String[] morbid_key_arr = morbidity_prob_map_all.keySet().toArray(new String[0]);

								for (String morbid_key : morbid_key_arr) {
									int grp_incl = ((Number) morbidity_setting_all.get(morbid_key)
											.get(SETTING_GRP_INCL)).intValue();
									morbidity_prob_map = morbidity_prob_map_all.get(morbid_key);

									if ((grp_incl & (1 << grp)) != 0) {
										Integer inf_id = Integer.parseInt(line[1]);
										if (morbidity_prob_map.containsKey(inf_id)) {

											ArrayList<Number[]> indivdual_morbidity_hist = new ArrayList<>();

											double[] morbidity_setting = morbidity_prob_map.get(inf_id);

											area_under_curve_by_morbidity_inf = area_under_curve_all.get(morbid_key)
													.get(inf_id);

											int past_inc_count = 0;

											for (int inf_hist_index = 2; (inf_hist_index
													+ 2) < line.length; inf_hist_index += 3) {
												int inf_start = Integer.parseInt(line[inf_hist_index]);
												int inf_end = Integer.parseInt(line[inf_hist_index + 1]);

												// Trim down duration (e.g. as in for PID)
												if (morbidity_setting[0] > 0) {
													inf_end = Math.min(inf_start + (int) morbidity_setting[0], inf_end);
												}

												int pt_sample_time = Arrays.binarySearch(sample_time, inf_end);
												if (pt_sample_time < 0) {
													pt_sample_time = ~pt_sample_time;
												}

												int numSetting = (morbidity_setting.length - 1) / 2;
												int pt_p = Arrays.binarySearch(morbidity_setting, 1, numSetting,
														past_inc_count);
												if (pt_p < 0) {
													pt_p = ~pt_p;
												}
												double prob_morbidity_per_day = morbidity_setting[numSetting + pt_p];

												Number[] morbidity_event_total = new Number[] { prob_morbidity_per_day,
														inf_start, inf_end };

												int pt_event = Collections.binarySearch(indivdual_morbidity_hist,
														morbidity_event_total, new Comparator<Number[]>() {
															@Override
															public int compare(Number[] o1, Number[] o2) {
																return Integer.compare(o1[1].intValue(),
																		o2[1].intValue());
															}
														});
												indivdual_morbidity_hist.add(~pt_event, morbidity_event_total);

												while (pt_sample_time >= 1 && inf_start < sample_time[pt_sample_time]) {
													int morbidity_dur_sample_time_start = Math.max(inf_start,
															sample_time[pt_sample_time - 1]);
													int morbidity_dur_sample_time_end = Math.min(inf_end,
															sample_time[pt_sample_time]);

													area_under_curve_by_morbidity_inf_sim = area_under_curve_by_morbidity_inf
															.get(simKey);

													if (area_under_curve_by_morbidity_inf_sim == null) {
														area_under_curve_by_morbidity_inf_sim = new HashMap<>();
														area_under_curve_by_morbidity_inf.put(simKey,
																area_under_curve_by_morbidity_inf_sim);
													}

													area_under_curve_by_morbidity_inf_sim_sampleTime = area_under_curve_by_morbidity_inf_sim
															.get(pt_sample_time);
													if (area_under_curve_by_morbidity_inf_sim_sampleTime == null) {
														area_under_curve_by_morbidity_inf_sim_sampleTime = new ArrayList<>();
														area_under_curve_by_morbidity_inf_sim.put(pt_sample_time,
																area_under_curve_by_morbidity_inf_sim_sampleTime);
													}

													Number[] morbidity_event = new Number[] { prob_morbidity_per_day,
															morbidity_dur_sample_time_start,
															morbidity_dur_sample_time_end };

													area_under_curve_by_morbidity_inf_sim_sampleTime
															.add(morbidity_event);

													pt_sample_time--;
												}

												past_inc_count++;
											} // End of checking infection history

											String[] birth_rate_suboutcome_name = (String[]) morbidity_setting_all
													.get(morbid_key).get(SETTING_BIRTH_BASED_SUBOUTCOMES);

											double[] birth_rate_by_age = (double[]) morbidity_setting_all
													.get(SETTING_BIRTH_RATE).get(SETTING_BIRTH_RATE);

											if (birth_rate_suboutcome_name != null
													&& indivdual_morbidity_hist.size() > 0) {

												// Switch time include:
												ArrayList<Integer> indiv_prob_switch_time = new ArrayList<>();

												// Valid time range
												indiv_prob_switch_time.add(enter_at);
												indiv_prob_switch_time.add(sample_time[sample_time.length - 1]);

												// Key = switch_time, V = Reason

												HashMap<Integer, Integer> map_switch_time_reason = new HashMap<>();

												// Add switch time for sampling time
												for (int switch_time_sample : sample_time) {
													map_switch_time_reason.put(switch_time_sample,
															1 << SWITCH_REASON_SAMPLE);
												}

												// Add switch time for change in birth rate due to aging
												for (int bAge = 0; bAge < birth_rate_by_age.length / 2; bAge++) {
													int switch_time_age = ((int) birth_rate_by_age[bAge]) - enter_age
															+ enter_at;
													int reason = map_switch_time_reason.getOrDefault(switch_time_age,
															0);
													map_switch_time_reason.put(switch_time_age,
															reason | (1 << SWITCH_REASON_AGING));
												}

												// Add switch time for end of infection
												HashMap<Integer, Number[]> map_moribity_end_time = new HashMap<>();
												ArrayList<Double> event_prob_arr_pre_sample_time = new ArrayList<>();

												for (Number[] morbidity_stat : indivdual_morbidity_hist) {
													int inf_end = morbidity_stat[2].intValue();
													map_moribity_end_time.put(inf_end, morbidity_stat);
													if (inf_end < sample_time[0]) {
														event_prob_arr_pre_sample_time
																.add(1 - Math.pow(1 - morbidity_stat[0].doubleValue(),
																		morbidity_stat[2].intValue()
																				- morbidity_stat[1].intValue()));
													}
													int reason = map_switch_time_reason.getOrDefault(inf_end, 0);
													map_switch_time_reason.put(inf_end,
															reason | (1 << SWITCH_REASON_END_PRIMARY_MORBIDITY));
												}
												// Finalise switch time array
												for (int sTime : map_switch_time_reason.keySet()) {
													int pt_sw = Collections.binarySearch(indiv_prob_switch_time, sTime);
													if (pt_sw < 0) {
														pt_sw = ~pt_sw;
														// Only add if they are in range
														if (pt_sw > 0 && pt_sw < indiv_prob_switch_time.size()) {
															indiv_prob_switch_time.add(pt_sw, sTime);
														}
													}
												}

												// Birth rate by age
												int age_at_s_time = enter_age
														+ (indiv_prob_switch_time.get(0) - enter_at);
												int birth_age_pt = Arrays.binarySearch(birth_rate_by_age, 0,
														birth_rate_by_age.length / 2, age_at_s_time);
												if (birth_age_pt < 0) {
													birth_age_pt = ~birth_age_pt;
												}
												double last_birth_rate = birth_rate_by_age[birth_age_pt
														+ birth_rate_by_age.length / 2];

												// Event probability
												HashMap<Integer, Double> prob_number_of_morbidity_event = new HashMap<>();
												prob_number_of_morbidity_event.put(0, 1.0);

												double[] area_sum_sample_time = new double[birth_rate_suboutcome_name.length];

												for (int switch_pt = 1; switch_pt < indiv_prob_switch_time
														.size(); switch_pt++) {
													int switch_time = indiv_prob_switch_time.get(switch_pt);
													int reason = map_switch_time_reason.get(switch_time);

													// Update area_sum_sample_time if within sample time
													if (switch_time > sample_time[0]
															&& indiv_prob_switch_time.get(switch_pt - 1) < exit_at) {

														// Update area_sum_sample_time
														for (int sub_outcome_index = 0; sub_outcome_index < birth_rate_suboutcome_name.length; sub_outcome_index++) {
															// Format:
															// {cat_0,cat_1,cat_2,...prob_cat_0,prob_cat_1,prob_cat_2...}
															double[] sub_outcome_setting = ((double[][]) morbidity_setting_all
																	.get(morbid_key)
																	.get(SETTING_BIRTH_BASED_SUBOUTCOMES_SETTING))[sub_outcome_index];

															double adj_prob_by_num_event = 0;
															for (Entry<Integer, Double> entry : prob_number_of_morbidity_event
																	.entrySet()) {
																if (entry.getKey() >= sub_outcome_setting[0]) {
																	// Should not have sub outcome if no morbidity so
																	// far
																	int pt_event = Arrays.binarySearch(
																			sub_outcome_setting, 0,
																			sub_outcome_setting.length / 2,
																			entry.getKey().doubleValue());
																	if (pt_event < 0) {
																		pt_event = ~pt_event;
																	}
																	adj_prob_by_num_event += entry.getValue()
																			* sub_outcome_setting[Math.min(pt_event
																					+ sub_outcome_setting.length / 2,
																					sub_outcome_setting.length - 1)];
																}
															}
															area_sum_sample_time[sub_outcome_index] += last_birth_rate
																	* adj_prob_by_num_event
																	* (Math.min(switch_time, exit_at)
																			- indiv_prob_switch_time
																					.get(switch_pt - 1));
														}
													}

													// Handle sample time
													if ((reason & (1 << SWITCH_REASON_SAMPLE)) != 0) {

														for (int sub_outcome_index = 0; sub_outcome_index < birth_rate_suboutcome_name.length; sub_outcome_index++) {
															String sub_outcome_name = birth_rate_suboutcome_name[sub_outcome_index];

															// Initialise sub_outcome array
															area_under_curve_by_morbidity = area_under_curve_all
																	.get(sub_outcome_name);
															if (area_under_curve_by_morbidity == null) {
																area_under_curve_by_morbidity = new HashMap<>();
																area_under_curve_all.put(sub_outcome_name,
																		area_under_curve_by_morbidity);
															}

															area_under_curve_by_morbidity_inf = area_under_curve_by_morbidity
																	.get(inf_id);
															if (area_under_curve_by_morbidity_inf == null) {
																area_under_curve_by_morbidity_inf = new HashMap<>();
																area_under_curve_by_morbidity.put(inf_id,
																		area_under_curve_by_morbidity_inf);
															}

															area_under_curve_by_morbidity_inf_sim = area_under_curve_by_morbidity_inf
																	.get(simKey);
															if (area_under_curve_by_morbidity_inf_sim == null) {
																area_under_curve_by_morbidity_inf_sim = new HashMap<>();
																area_under_curve_by_morbidity_inf.put(simKey,
																		area_under_curve_by_morbidity_inf_sim);
															}

															int pt_sample_time = Arrays.binarySearch(sample_time,
																	switch_time);

															area_under_curve_by_morbidity_inf_sim_sampleTime = area_under_curve_by_morbidity_inf_sim
																	.get(pt_sample_time);
															if (area_under_curve_by_morbidity_inf_sim_sampleTime == null) {
																area_under_curve_by_morbidity_inf_sim_sampleTime = new ArrayList<>();
																area_under_curve_by_morbidity_inf_sim.put(
																		pt_sample_time,
																		area_under_curve_by_morbidity_inf_sim_sampleTime);
															}

															if (area_sum_sample_time[sub_outcome_index] > 0) {
																Number[] suboutcome_area_under_curve = new Number[] {
																		area_sum_sample_time[sub_outcome_index], 0, 1 };
																area_under_curve_by_morbidity_inf_sim_sampleTime
																		.add(suboutcome_area_under_curve);
															}

															// Clear store
															area_sum_sample_time[sub_outcome_index] = 0;
														}
													}
													// Handle aging
													if ((reason & (1 << SWITCH_REASON_AGING)) != 0
															&& switch_time < exit_at) {
														age_at_s_time = enter_age + (switch_time - enter_at) + 1;
														birth_age_pt = Arrays.binarySearch(birth_rate_by_age, 0,
																birth_rate_by_age.length / 2, age_at_s_time);
														if (birth_age_pt < 0) {
															birth_age_pt = ~birth_age_pt;
														}
														last_birth_rate = birth_rate_by_age[birth_age_pt
																+ birth_rate_by_age.length / 2];
													}

													// Handle morbidity incidence
													if ((reason & (1 << SWITCH_REASON_END_PRIMARY_MORBIDITY)) != 0) {
														Number[] morbidity_stat = map_moribity_end_time
																.get(switch_time);

														double prob_morbidity_occured = 1
																- Math.pow(1 - morbidity_stat[0].doubleValue(),
																		morbidity_stat[2].intValue()
																				- morbidity_stat[1].intValue());

														Integer[] event_count_arr = prob_number_of_morbidity_event
																.keySet().toArray(new Integer[0]);
														Arrays.sort(event_count_arr);

														double preProb = 0;
														for (int event_count : event_count_arr) {
															double preProb_store = prob_number_of_morbidity_event
																	.get(event_count);
															prob_number_of_morbidity_event.put(event_count,
																	prob_number_of_morbidity_event.get(event_count)
																			* (1 - prob_morbidity_occured)
																			+ preProb * prob_morbidity_occured);
															preProb = preProb_store;
														}

														prob_number_of_morbidity_event.put(
																event_count_arr[event_count_arr.length - 1] + 1,
																preProb * prob_morbidity_occured);

													}
												}
											} // End of if (birth_rate_suboutcome_name != null) {
										} // End of if (pid_prob_map.containsKey(inf_id)) {
									} // End of if ((grp_incl & (1 << grp)) != 0) {
								} // End of for(String morbid_key : morbidity_prob_map_all.keySet()) {
							}
						} // End of for (String[] line : ent.getValue()) {

					} // for (Entry<String, ArrayList<String[]>> ent : linesMap.entrySet()) {
				} // End of if (inf_hist_zips.length != 1) { } else {

				System.out.printf("Looking at infection history at %s. Time req. = %.3fs\n", singleResultSet.getName(),
						(System.currentTimeMillis() - tic) / 1000.0);
			} // End of for (File singleResultSet : singleResultSets) {
		} // End of for (File resultSetDir : scenario_dirs_incl) {

		// Print Result
		for (String morbid_key : area_under_curve_all.keySet()) {

			for (Entry<Integer, HashMap<String, HashMap<Integer, ArrayList<Number[]>>>> area_under_curve : area_under_curve_all
					.get(morbid_key).entrySet()) {

				if (area_under_curve.getValue().size() > 0) {
					PrintWriter pWri_PID_stat = new PrintWriter(
							new File(output_dir, String.format(morbid_key, area_under_curve.getKey())));

					StringBuilder header = new StringBuilder();
					header.append("SIM_ID");
					for (int i = 0; i < sample_time.length - 1; i++) {
						String time_range = String.format("(%d - %d)", sample_time[i + 1], sample_time[i]);
						header.append(',');
						header.append("Incidence_" + time_range);
						header.append(',');
						header.append("Prob_Morbidity_" + time_range);
					}
					pWri_PID_stat.println(header.toString());

					String[] sim_keys = area_under_curve.getValue().keySet().toArray(new String[0]);
					Arrays.sort(sim_keys, new Comparator<String>() {
						@Override
						public int compare(String o1, String o2) {
							Pattern key_pattern = Pattern.compile("(.*):(.*)\\((-?\\\\d+)_(-?\\\\d+)_(-?\\\\d+)\\)");
							Matcher m1 = key_pattern.matcher(o1);
							Matcher m2 = key_pattern.matcher(o2);
							if (m1.find() && m2.find()) {
								int res = m1.group(1).compareTo(m2.group(1));
								if (res == 0) {
									res = m1.group(2).compareTo(m2.group(2));
								}
								for (int s = 3; s < Math.min(m1.groupCount(), m2.groupCount()) && res == 0; s++) {
									res = Long.compare(Long.parseLong(m1.group(s)), Long.parseLong(m2.group(s)));
								}

								return res;

							} else {
								return o1.compareTo(o2);
							}
						}
					});
					for (String sim_key : sim_keys) {
						area_under_curve_by_morbidity_inf_sim = area_under_curve.getValue().get(sim_key);

						Integer[] time_range_index = area_under_curve_by_morbidity_inf_sim.keySet()
								.toArray(new Integer[0]);
						Arrays.sort(time_range_index);

						pWri_PID_stat.printf("%s", sim_key);

						for (Integer tR_i : time_range_index) {
							double pSum = 0;
							for (Number[] morbidity_stat : area_under_curve_by_morbidity_inf_sim.get(tR_i)) {
								pSum += 1 - Math.pow(1 - morbidity_stat[0].doubleValue(),
										morbidity_stat[2].intValue() - morbidity_stat[1].intValue());
							}
							pWri_PID_stat.printf(",%d,%s", area_under_curve_by_morbidity_inf_sim.get(tR_i).size(),
									pSum);
						}

						pWri_PID_stat.println();

					}

					pWri_PID_stat.close();

				}
			}
		}
	}

}
