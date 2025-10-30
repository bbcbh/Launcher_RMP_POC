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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import sim.Abstract_Runnable_ClusterModel;
import sim.Runnable_MetaPopulation_Transmission_RMP_MultiInfection;
import sim.Simulation_ClusterModelTransmission;

public class Util_Analyse_RMP {

	public static void extract_inf_num_infection_to_csv(File scenario_dirs_incl, int[][] colIndex)
			throws IOException, FileNotFoundException {
		extract_inf_stat_to_csv(new File[] { scenario_dirs_incl }, scenario_dirs_incl, colIndex,
				"Infectious_Prevalence_Person_", "Inf_%d_num_of_infected.csv");
	}

	public static void extract_inf_incidence_to_csv(File scenario_dirs_incl, int[][] colIndex)
			throws IOException, FileNotFoundException {
		extract_inf_stat_to_csv(new File[] { scenario_dirs_incl }, scenario_dirs_incl, colIndex, "Incidence_Person_",
				"Inf_%d_cumul_incidence.csv");
	}

	public static void extract_inf_stat_to_csv(File[] scenario_dirs_incl, File output_dir, int[][] colIndex,
			String csvFeader, String fileOutputFormat) throws IOException, FileNotFoundException {
		Comparator<File> cmp_file_suffix = generate_file_comparator_by_suffix();

		ArrayList<String> array_qsub = new ArrayList<>();
		ArrayList<ArrayList<StringBuilder>> lines_all_inf = new ArrayList<>();
		for (int p = 0; p < colIndex.length; p++) {
			lines_all_inf.add(new ArrayList<>());
		}

		for (File resultSetDir : scenario_dirs_incl) {
			File[] singleResultSets = resultSetDir.listFiles(new FileFilter() {
				@Override
				public boolean accept(File pathname) {
					return pathname.isDirectory() && pathname.getName().startsWith(resultSetDir.getName());
				}
			});

			Arrays.sort(singleResultSets, cmp_file_suffix);

			Pattern pattern_num_inf_src = Pattern.compile(csvFeader + "(-?\\d+).csv.7z");

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
					array_qsub.add(String.format("qsub %s.pbs\n", singleResultSet.getName()));
					completedSet = false;
				} else if (zips[0].length() == 0) {
					System.err.printf("Error. Zip file of length 0 in %s != 1\n", singleResultSet.getName());
					array_qsub.add(String.format("qsub %s.pbs\n", singleResultSet.getName()));
					completedSet = false;
				}

			}

			if (!completedSet) {
				for (String resub : array_qsub) {
					System.out.print(resub);
				}

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

				Pattern pattern_inf_stat_header = Pattern
						.compile("\\[(.+),(\\d+)\\]" + csvFeader + "(-?\\d+)_(-?\\d+).csv");

				String[] keys = linesMap.keySet().toArray(new String[0]);
				Arrays.sort(keys, new Comparator<String>() {
					@Override
					public int compare(String o1, String o2) {
						Matcher m1 = pattern_inf_stat_header.matcher(o1);
						Matcher m2 = pattern_inf_stat_header.matcher(o2);
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
								Matcher m = pattern_inf_stat_header.matcher(key);
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
				pWriters_num_infect[p] = new PrintWriter(new File(output_dir, String.format(fileOutputFormat, p)));
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
	public final static String SETTING_INF_INCL = "SETTING_INF_INCL"; // Integer
	public final static String SETTING_SAMPLE_FREQ = "SETTING_SAMPLE_FREQ"; // Integer
	public final static String SETTING_EVENT_COUNT_LIMIT = "SETTING_EVENT_COUNT_LIMIT"; // Integer

	public final static String SETTING_RATE_ADJ_BY_AGE = "SETTING_BIRTH_RATE"; // double[]
	public final static String SETTING_SUBOUTCOMES = "SETTING_BIRTH_BASED_SUBOUTCOMES"; // String[]
	public final static String SETTING_SUBOUTCOMES_SETTING = "SETTING_BIRTH_BASED_SUBOUTCOMES_SETTING"; // double[]

	public final static Pattern sim_key_pattern = Pattern.compile("(.*):.*_(\\d+)\\((-?\\d+)_(-?\\d+)_(-?\\d+)\\)");

	// morbidity_setting_all = Number[] {grp_incl}
	public static void extracted_InfectHist(File[] sce_dir, File output_dir, int[] sample_time,
			HashMap<String, ArrayList<String>> sim_sel_map, HashMap<Long, HashMap<Integer, String[]>> demographic,
			Map<String, Map<Integer, double[]>> morbidity_prob_map_all,
			Map<String, Map<String, Object>> morbidity_setting_all) throws IOException, FileNotFoundException {
		Pattern pattern_inf_hist_zip = Pattern.compile(
				Simulation_ClusterModelTransmission.FILENAME_INFECTION_HISTORY_ZIP.replaceAll("%d", "(-?\\\\d+)"));
		Pattern pattern_inf_preval_header = Pattern.compile(String.format("\\[(.+),(\\d+)\\]%s",
				Simulation_ClusterModelTransmission.FILENAME_INFECTION_HISTORY.replaceAll("%d", "(-?\\\\d+)")));

		final int GRP_INDEX_INF_HISTORY_HEADER_ROW_INDEX = 2;
		final int GRP_INDEX_INF_HISTORY_HEADER_CMAP_SEED = 3;
		final int GRP_INDEX_INF_HISTORY_HEADER_SIM_SEED = 4;

		// Key

		// Lv 1: (String) sim_key
		// Lv 2: (Integer) person_id
		// Value: [Daily_Event_Probabiliy,From_Time,To_Time] or [Infect_start,
		// Infect_end, infId]

		HashMap<String, HashMap<Integer, ArrayList<Number[]>>> infection_hist_extract_by_inf_simkey = new HashMap<>();
		HashMap<Integer, ArrayList<Number[]>> infection_hist_extract_by_inf_simkey_person_id;
		ArrayList<Number[]> infection_hist_extract_entry;

		Map<Integer, double[]> morbidity_prob_map;

		String[] dir_suffix_sel = sim_sel_map.keySet().toArray(new String[0]);
		Arrays.sort(dir_suffix_sel);

		for (File resultSetDir : sce_dir) {

			// Key = morbidkey_inf_id
			HashMap<String, PrintWriter> priWriter_map = new HashMap<>();
			File[] singleResultSets = resultSetDir.listFiles(new FileFilter() {
				@Override
				public boolean accept(File pathname) {
					boolean res = pathname.isDirectory();
					res &= pathname.getName().startsWith(resultSetDir.getName());
					if (res && dir_suffix_sel.length > 0) { // Otherwise include all
						String suffix = pathname.getName().substring(resultSetDir.getName().length() + 1);
						res &= Arrays.binarySearch(dir_suffix_sel, suffix) >= 0;
					}
					return res;
				}

			});

			for (File singleResultSet : singleResultSets) {
				long tic = System.currentTimeMillis();
				String sim_sel_key = singleResultSet.getName().substring(resultSetDir.getName().length() + 1);
				String[] sim_sel_values = null;

				if (sim_sel_map.containsKey(sim_sel_key)) {
					sim_sel_values = sim_sel_map.get(sim_sel_key).toArray(new String[0]);
					Arrays.sort(sim_sel_values);
				}

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
						String simKey, seedIdentifier;

						if (m.find()) {
							seedIdentifier = String.format("(%s_%s_%s)",
									m.group(GRP_INDEX_INF_HISTORY_HEADER_ROW_INDEX),
									m.group(GRP_INDEX_INF_HISTORY_HEADER_CMAP_SEED),
									m.group(GRP_INDEX_INF_HISTORY_HEADER_SIM_SEED));
							simKey = String.format("%s:%s%s", resultSetDir.getName(), singleResultSet.getName(),
									seedIdentifier);

						} else {
							seedIdentifier = String.format("(%s)", ent.getKey());
							simKey = String.format("%s:%s_%s", resultSetDir.getName(), singleResultSet.getName(),
									seedIdentifier);
						}

						if (sim_sel_values == null || Arrays.binarySearch(sim_sel_values, seedIdentifier) >= 0) {

							for (String[] line : ent.getValue()) {
								int person_id = Integer.parseInt(line[0]);
								int inf_id = Integer.parseInt(line[1]);
								String[] indiv_stat = lookup_demograhic.get(person_id);

								int enter_at = Integer.parseInt(
										indiv_stat[Abstract_Runnable_ClusterModel.POP_INDEX_ENTER_POP_AT + 1]);
								int exit_at = Integer
										.parseInt(indiv_stat[Abstract_Runnable_ClusterModel.POP_INDEX_EXIT_POP_AT + 1]);

								// Only include individual within sample time window
								if (sample_time[0] <= exit_at && enter_at <= sample_time[sample_time.length - 1]) {

									infection_hist_extract_by_inf_simkey_person_id = infection_hist_extract_by_inf_simkey
											.get(simKey);

									if (infection_hist_extract_by_inf_simkey_person_id == null) {
										infection_hist_extract_by_inf_simkey_person_id = new HashMap<>();
										infection_hist_extract_by_inf_simkey.put(simKey,
												infection_hist_extract_by_inf_simkey_person_id);
									}
									for (int inf_hist_index = 2; (inf_hist_index
											+ 2) < line.length; inf_hist_index += 3) {
										int inf_start = Integer.parseInt(line[inf_hist_index]);
										int inf_end = inf_hist_index + 1 < line.length
												? Integer.parseInt(line[inf_hist_index + 1])
												: Math.min(exit_at, sample_time[sample_time.length - 1]);
										int treatment_type = inf_hist_index + 2 < line.length
												? Integer.parseInt(line[inf_hist_index + 2])
												: 0;

										// Include infection if in range, or morbidity_prob_map has
										// entry
										if (treatment_type != Runnable_MetaPopulation_Transmission_RMP_MultiInfection.INFECTION_HIST_OVERTREATMENT) {

											infection_hist_extract_entry = infection_hist_extract_by_inf_simkey_person_id
													.get(person_id);

											if (infection_hist_extract_entry == null) {
												infection_hist_extract_entry = new ArrayList<>();
												infection_hist_extract_by_inf_simkey_person_id.put(person_id,
														infection_hist_extract_entry);
											}
											infection_hist_extract_entry
													.add(new Number[] { inf_start, inf_end, inf_id });
										}
									}
								} // End of if (sample_time[0] <= exit_at && enter_at <=
									// sample_time[sample_time.length - 1]) {
							} // End of for (String[] line : ent.getValue()) {
						} // End of if (sim_sel_values == null || Arrays.binarySearch(sim_sel_values,
							// seedIdentifier) >= 0)
					} // End of looking up inf_hist_zips
				} // End of if (inf_hist_zips.length == 1)

				System.out.printf("Looking at infection history at %s. Time req. = %.3fs\n", singleResultSet.getName(),
						(System.currentTimeMillis() - tic) / 1000.0);

				// Print Result (per set version)

				for (String morbid_key : morbidity_prob_map_all.keySet()) {

					String priWriterMapKey = morbid_key;
					PrintWriter[] pWri_infect_hist_arr;

					int inf_incl = ((Number) morbidity_setting_all.get(morbid_key).get(SETTING_INF_INCL)).intValue();
					int grp_incl = ((Number) morbidity_setting_all.get(morbid_key).get(SETTING_GRP_INCL)).intValue();

					// Set up PrintWriters

					// Main
					ArrayList<String> priWriter_keys_list = new ArrayList<>(List.of(priWriterMapKey));
					ArrayList<String> fNames_list = new ArrayList<>(List.of(morbid_key));

					// Birth rate dependent sub_outcome
					String[] morbidity_sub_outcome = (String[]) morbidity_setting_all.get(morbid_key)
							.get(SETTING_SUBOUTCOMES);
					if (morbidity_sub_outcome != null) {
						for (String sub_outcome_name : morbidity_sub_outcome) {
							priWriter_keys_list.add(String.format("%s_%s", morbid_key, sub_outcome_name));
							fNames_list.add(sub_outcome_name);
						}

					}
					pWri_infect_hist_arr = new PrintWriter[priWriter_keys_list.size()];
					for (int i = 0; i < pWri_infect_hist_arr.length; i++) {
						pWri_infect_hist_arr[i] = priWriter_map.get(priWriter_keys_list.get(i));
						if (pWri_infect_hist_arr[i] == null) {
							pWri_infect_hist_arr[i] = new PrintWriter(new File(output_dir, fNames_list.get(i)));
							pWri_infect_hist_arr[i].print("SIM_ID");
							int time_gap = 1;
							if (morbidity_setting_all.get(morbid_key).containsKey(SETTING_SAMPLE_FREQ)) {
								time_gap = ((Number) morbidity_setting_all.get(morbid_key).get(SETTING_SAMPLE_FREQ))
										.intValue();

							}
							int s_time = sample_time[0];
							while (s_time <= sample_time[sample_time.length - 1]) {
								pWri_infect_hist_arr[i].print(',');
								pWri_infect_hist_arr[i].print(s_time);
								s_time += time_gap;
							}
							pWri_infect_hist_arr[i].println();
							priWriter_map.put(priWriter_keys_list.get(i), pWri_infect_hist_arr[i]);
							pWri_infect_hist_arr[i].flush();
						}
					}

					String[] sim_keys = infection_hist_extract_by_inf_simkey.keySet().toArray(new String[0]);
					Arrays.sort(sim_keys, new Comparator<String>() {
						@Override
						public int compare(String o1, String o2) {
							Matcher m1 = sim_key_pattern.matcher(o1);
							Matcher m2 = sim_key_pattern.matcher(o2);
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
						infection_hist_extract_by_inf_simkey_person_id = infection_hist_extract_by_inf_simkey
								.get(sim_key);

						Matcher m_simkey = sim_key_pattern.matcher(sim_key);

						if (!m_simkey.find()) {
							System.err.printf("Ill-formed simKey='%s'.Entry skipped.\n", sim_key);
						} else {
							int time_gap = 1;
							boolean cumulative_count = false;
							int infection_count_limit = -1;

							if (morbidity_setting_all.get(morbid_key).containsKey(SETTING_SAMPLE_FREQ)) {
								cumulative_count = true;
								time_gap = ((Number) morbidity_setting_all.get(morbid_key).get(SETTING_SAMPLE_FREQ))
										.intValue();

								if (morbidity_setting_all.get(morbid_key).containsKey(SETTING_EVENT_COUNT_LIMIT)) {
									infection_count_limit = ((Integer) morbidity_setting_all.get(morbid_key)
											.get(SETTING_EVENT_COUNT_LIMIT)).intValue();
								}

							}

							ArrayList<Integer> sample_time_list = new ArrayList<>();

							int s_time = sample_time[0];
							while (s_time <= sample_time[sample_time.length - 1]) {
								sample_time_list.add(s_time);
								s_time += time_gap;
							}

							Long cMapSeed = Long.parseLong(m_simkey.group(4));

							if (morbidity_prob_map_all.get(morbid_key).isEmpty()) {
								int[] data_by_sample_time = new int[sample_time_list.size()];
								int[] indivdual_infected_record = new int[data_by_sample_time.length];

								for (Entry<Integer, ArrayList<Number[]>> inf_hist_ent : infection_hist_extract_by_inf_simkey_person_id
										.entrySet()) {
									int person_id = inf_hist_ent.getKey();
									String[] indiv_stat = demographic.get(cMapSeed).get(person_id);
									int grp = Integer
											.parseInt(indiv_stat[Abstract_Runnable_ClusterModel.POP_INDEX_GRP + 1]);

									if ((1 << grp & grp_incl) != 0) {
										int enter_age = Integer.parseInt(
												indiv_stat[Abstract_Runnable_ClusterModel.POP_INDEX_ENTER_POP_AGE + 1]);
										int enter_at = Integer.parseInt(
												indiv_stat[Abstract_Runnable_ClusterModel.POP_INDEX_ENTER_POP_AT + 1]);
										int exit_at = Integer.parseInt(
												indiv_stat[Abstract_Runnable_ClusterModel.POP_INDEX_EXIT_POP_AT + 1]);

										ArrayList<Number[]> inf_hist = inf_hist_ent.getValue();
										Arrays.fill(indivdual_infected_record, 0);

										// inf_event format: new Number[] { inf_start, inf_end, inf_id }
										for (Number[] inf_event : inf_hist) {
											if ((inf_incl & 1 << inf_event[2].intValue()) != 0) {
												if (cumulative_count) {
													int t_pt = Collections.binarySearch(sample_time_list,
															inf_event[0].intValue());
													if (t_pt < 0) {
														t_pt = ~t_pt;
													}
													if (sample_time_list.get(t_pt) - time_gap < inf_event[0].intValue()
															&& t_pt < sample_time_list.size()) {
														indivdual_infected_record[t_pt]++;
														if (infection_count_limit == -1) {
															// New infection
															data_by_sample_time[t_pt]++;
														} else {
															if (indivdual_infected_record[t_pt] == infection_count_limit) {
																data_by_sample_time[t_pt]++;
															}
														}

													}
												} else { // Measure prevalence
													int sTime = inf_event[0].intValue();
													int t_pt = -1;

													while (sTime < Math.min(inf_event[1].intValue(), exit_at)) {
														if (sTime >= sample_time_list.get(0).intValue()
																&& sTime <= sample_time_list
																		.get(sample_time_list.size() - 1)) {
															if (t_pt == -1) {
																// Find the first t_pt
																t_pt = Collections.binarySearch(sample_time_list,
																		sTime);
															}
															if (indivdual_infected_record[t_pt] == 0) {
																data_by_sample_time[t_pt]++;
															}
															indivdual_infected_record[t_pt]++;

														}
														sTime++;
														if (t_pt != -1) {
															t_pt++;
														}
													}
												}
											}
										}
									}
								}
								for (PrintWriter pWri : pWri_infect_hist_arr) {
									pWri.printf("%s", sim_key);
								}

								for (int i = 0; i < data_by_sample_time.length; i++) {
									pWri_infect_hist_arr[0].printf(",%d", data_by_sample_time[i]);
								}
								pWri_infect_hist_arr[0].println();

							} else { // !morbidity_prob_map_all.get(morbid_key).isEmpty()
								morbidity_prob_map = morbidity_prob_map_all.get(morbid_key);
								// Main outcome
								double[] prob_morbidity_main = new double[sample_time_list.size()];
								// Sub outcomes
								double[][] cumul_prob_morbidity_suboutcomes = new double[0][];

								if (morbidity_sub_outcome != null) {
									cumul_prob_morbidity_suboutcomes = new double[morbidity_sub_outcome.length][sample_time_list
											.size()];

								}

								for (Entry<Integer, ArrayList<Number[]>> inf_hist_ent : infection_hist_extract_by_inf_simkey_person_id
										.entrySet()) {

									int person_id = inf_hist_ent.getKey();
									String[] indiv_stat = demographic.get(cMapSeed).get(person_id);
									int enter_age = Integer.parseInt(
											indiv_stat[Abstract_Runnable_ClusterModel.POP_INDEX_ENTER_POP_AGE + 1]);
									int enter_at = Integer.parseInt(
											indiv_stat[Abstract_Runnable_ClusterModel.POP_INDEX_ENTER_POP_AT + 1]);
									int exit_at = Integer.parseInt(
											indiv_stat[Abstract_Runnable_ClusterModel.POP_INDEX_EXIT_POP_AT + 1]);
									int past_inc_count = 0;

									HashMap<Integer, Double> prob_number_of_morbidity_event = new HashMap<>();
									prob_number_of_morbidity_event.put(0, 1.0);

									// inf_event = new Number[] { inf_start, inf_end, inf_id }

									int[][] inf_hist_adj = new int[inf_hist_ent.getValue().size()][3];

									int pt_inf_hist_adj = 0;
									// Trim down duration (e.g. as in for PID)

									for (Number[] inf_event : inf_hist_ent.getValue()) {
										inf_hist_adj[pt_inf_hist_adj][0] = inf_event[0].intValue();
										inf_hist_adj[pt_inf_hist_adj][1] = inf_event[1].intValue();
										inf_hist_adj[pt_inf_hist_adj][2] = inf_event[2].intValue();
										double[] morbidity_setting = morbidity_prob_map
												.get(inf_hist_adj[pt_inf_hist_adj][2]);

										if (morbidity_setting[0] > 0) {
											inf_hist_adj[pt_inf_hist_adj][1] = Math.min(
													inf_hist_adj[pt_inf_hist_adj][0] + (int) morbidity_setting[0],
													inf_hist_adj[pt_inf_hist_adj][1]);
										}

										pt_inf_hist_adj++;
									}
									Arrays.sort(inf_hist_adj, new Comparator<int[]>() {
										@Override
										public int compare(int[] o1, int[] o2) {
											int res = 0;
											for (int i = 0; i < Math.min(o1.length, o2.length) && res == 0; i++) {
												res = Integer.compare(o1[i], o2[i]);
											}
											return res;
										}
									});

									// Key = end inf time, V = prob_morbidity_by_inf_end
									HashMap<Integer, ArrayList<Double>> main_morbidity_rec = new HashMap<>();

									for (int[] inf_event : inf_hist_adj) {
										int inf_start = inf_event[0];
										if (inf_start < sample_time_list.get(sample_time_list.size() - 1)) {
											int inf_end = Math.min(sample_time_list.get(sample_time_list.size() - 1),
													inf_event[1]);
											int inf_id = inf_event[2];
											double[] morbidity_setting = morbidity_prob_map.get(inf_id);
											int numSetting = (morbidity_setting.length - 1) / 2;
											int pt_p = Arrays.binarySearch(morbidity_setting, 1, numSetting,
													past_inc_count);
											if (pt_p < 0) {
												pt_p = ~pt_p;
											}
											double prob_morbidity_per_day = morbidity_setting[numSetting + pt_p];
											double prob_morbidity_by_inf_end = 1
													- Math.pow(1 - prob_morbidity_per_day, inf_end - inf_start);

											// Update event count probability for pre-sample time
											if (inf_end < sample_time_list.get(0)) {
												update_prob_number_morbidity_event(prob_number_of_morbidity_event,
														prob_morbidity_by_inf_end);
											} else {
												int pt_inf_end = Collections.binarySearch(sample_time_list, inf_end);
												if (pt_inf_end < 0) {
													pt_inf_end = ~pt_inf_end;
												}

												// Calculate the probability of main morbidity
												prob_morbidity_main[pt_inf_end] += prob_morbidity_by_inf_end;

												ArrayList<Double> prob = main_morbidity_rec.get(inf_end);
												if (prob == null) {
													prob = new ArrayList<>();
													main_morbidity_rec.put(inf_end, prob);
												}
												prob.add(prob_morbidity_by_inf_end);
											}
											past_inc_count++;
										}

									} // End of checking single individual infection history

									if (morbidity_sub_outcome != null) {
										int start_time = Math.max(enter_at, sample_time_list.get(0));
										int end_time = Math.min(exit_at,
												sample_time_list.get(sample_time_list.size() - 1));

										Integer[] main_morbidity_rec_time = main_morbidity_rec.keySet()
												.toArray(new Integer[0]);
										Arrays.sort(main_morbidity_rec_time);

										int morbidity_rec_time_pt = 0;
										int sample_time_rec_pt = Collections.binarySearch(sample_time_list, start_time);
										if (sample_time_rec_pt < 0) {
											sample_time_rec_pt = ~sample_time_rec_pt;
										}

										double[][] adj_rate_by_age = new double[morbidity_sub_outcome.length][];
										int[] adj_rate_pt = new int[adj_rate_by_age.length];
										Arrays.fill(adj_rate_pt, -1);

										// Set up birth rate look up if needed
										for (int sub_outcome_index = 0; sub_outcome_index < morbidity_sub_outcome.length; sub_outcome_index++) {
											String sub_outcome_name = morbidity_sub_outcome[sub_outcome_index];
											adj_rate_by_age[sub_outcome_index] = (double[]) morbidity_setting_all
													.get(sub_outcome_name).get(SETTING_RATE_ADJ_BY_AGE);

											if (adj_rate_by_age[sub_outcome_index] != null) {
												int age_start = enter_age + start_time - enter_at;
												adj_rate_pt[sub_outcome_index] = Arrays.binarySearch(
														adj_rate_by_age[sub_outcome_index], 0,
														adj_rate_by_age[sub_outcome_index].length / 2, age_start);
												if (adj_rate_pt[sub_outcome_index] < 0) {
													adj_rate_pt[sub_outcome_index] = ~adj_rate_pt[sub_outcome_index];
												}
											}
										}
										double[] adj_prob_by_num_event_suboutcome = new double[morbidity_sub_outcome.length];
										Arrays.fill(adj_prob_by_num_event_suboutcome, Double.NaN);

										int start_time_index = Collections.binarySearch(sample_time_list, start_time);
										if (start_time_index < 0) {
											start_time_index = ~start_time_index;
										}

										int end_time_index = Collections.binarySearch(sample_time_list, end_time);
										if (end_time_index < 0) {
											end_time_index = ~end_time_index;
										}

										for (int sTime_pt = start_time_index; sTime_pt <= end_time_index; sTime_pt++) {

											int sTime = sample_time_list.get(sTime_pt);

											while (morbidity_rec_time_pt < main_morbidity_rec_time.length
													&& main_morbidity_rec_time[morbidity_rec_time_pt]
															.intValue() <= sTime) {
												for (Double prob : main_morbidity_rec
														.get(main_morbidity_rec_time[morbidity_rec_time_pt])) {
													if (prob != null) {
														update_prob_number_morbidity_event(
																prob_number_of_morbidity_event, prob.doubleValue());
													}
												}

												morbidity_rec_time_pt++;

												// Recalculate adj_prob_by_num_event_suboutcome
												Arrays.fill(adj_prob_by_num_event_suboutcome, Double.NaN);
											}

											for (int sub_outcome_index = 0; sub_outcome_index < morbidity_sub_outcome.length; sub_outcome_index++) {
												String sub_outcome_name = morbidity_sub_outcome[sub_outcome_index];
												double rate_adj = 1;
												// Check if birth_rate adjustment is needed
												if (adj_rate_by_age[sub_outcome_index] != null) {
													double[] adj_rate = adj_rate_by_age[sub_outcome_index];

													int age_sTime = enter_age + sTime - enter_at;
													while (adj_rate_pt[sub_outcome_index] < adj_rate.length / 2
															&& age_sTime > adj_rate[adj_rate_pt[sub_outcome_index]]) {
														adj_rate_pt[sub_outcome_index]++;
													}
													rate_adj = adj_rate[adj_rate.length / 2
															+ adj_rate_pt[sub_outcome_index]];
												}

												// Format:
												// {cat_0,cat_1,cat_2,...prob_cat_0,prob_cat_1,prob_cat_2...}
												double[] sub_outcome_setting = ((double[]) morbidity_setting_all
														.get(sub_outcome_name).get(SETTING_SUBOUTCOMES_SETTING));

												if (Double.isNaN(adj_prob_by_num_event_suboutcome[sub_outcome_index])) {

													double adj_prob_by_num_event = 0;
													for (Entry<Integer, Double> entry : prob_number_of_morbidity_event
															.entrySet()) {
														if (entry.getKey() >= sub_outcome_setting[0]) {
															// Should not have sub outcome if no
															// morbidity so far
															int pt_event = Arrays.binarySearch(sub_outcome_setting, 0,
																	sub_outcome_setting.length / 2,
																	entry.getKey().doubleValue());
															if (pt_event < 0) {
																pt_event = ~pt_event;
															}
															adj_prob_by_num_event += entry.getValue()
																	* sub_outcome_setting[Math.min(
																			pt_event + sub_outcome_setting.length / 2,
																			sub_outcome_setting.length - 1)];
														}
													}

													adj_prob_by_num_event_suboutcome[sub_outcome_index] = adj_prob_by_num_event;
												}

												if (sTime_pt < sample_time_list.size() && (rate_adj
														* adj_prob_by_num_event_suboutcome[sub_outcome_index] > 0)) {

													cumul_prob_morbidity_suboutcomes[sub_outcome_index][sTime_pt] += rate_adj
															* adj_prob_by_num_event_suboutcome[sub_outcome_index];
												}
											}

										}
									}

								}
								// End of checking end all individual
								// i.e., for (Entry<Integer, ArrayList<Number[]>> inf_hist_ent :
								// infection_hist_extract_by_inf_simkey_intIndex.entrySet()) {

								for (PrintWriter pWri : pWri_infect_hist_arr) {
									pWri.printf("%s", sim_key);
								}
								for (int i = 0; i < prob_morbidity_main.length; i++) {
									pWri_infect_hist_arr[0].printf(",%f", prob_morbidity_main[i]);
								}
								pWri_infect_hist_arr[0].println();
								if (morbidity_sub_outcome != null) {
									for (int sub_outcome_index = 0; sub_outcome_index < morbidity_sub_outcome.length; sub_outcome_index++) {
										double[] sub_outcome = cumul_prob_morbidity_suboutcomes[sub_outcome_index];
										for (int i = 0; i < sub_outcome.length; i++) {
											pWri_infect_hist_arr[sub_outcome_index + 1].printf(",%f", sub_outcome[i]);
										}
										pWri_infect_hist_arr[sub_outcome_index + 1].println();
									}
								}
							}

						}
						for (PrintWriter pWri : pWri_infect_hist_arr) {
							pWri.flush();
						}
					}

				}
			} // End of for (File singleResultSet : singleResultSets) {

			for (PrintWriter pri : priWriter_map.values()) {
				pri.close();
			}

		} // End of for (File resultSetDir : scenario_dirs_incl) {

	}

	private static void update_prob_number_morbidity_event(HashMap<Integer, Double> prob_number_of_morbidity_event,
			double prob_morbidity_by_inf_end) {
		Integer[] event_count_arr = prob_number_of_morbidity_event.keySet().toArray(new Integer[0]);
		Arrays.sort(event_count_arr);
		double preProb = 0;
		for (int event_count : event_count_arr) {
			double preProb_store = prob_number_of_morbidity_event.get(event_count);
			prob_number_of_morbidity_event.put(event_count,
					prob_number_of_morbidity_event.get(event_count) * (1 - prob_morbidity_by_inf_end)
							+ preProb * prob_morbidity_by_inf_end);
			preProb = preProb_store;
		}

		prob_number_of_morbidity_event.put(event_count_arr[event_count_arr.length - 1] + 1,
				preProb * prob_morbidity_by_inf_end);
	}

}
