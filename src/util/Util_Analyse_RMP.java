package util;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import test.Test_Common_Static_Vars;

public class Util_Analyse_RMP {

	public static void extract_num_infection_to_csv(File scenario_dirs_incl, int[][] colIndex) throws IOException, FileNotFoundException {
		extract_num_infection_to_csv(new File[] {scenario_dirs_incl}, scenario_dirs_incl, colIndex);
	}
	public static void extract_num_infection_to_csv(File[] scenario_dirs_incl, File output_dir,int[][] colIndex ) throws IOException, FileNotFoundException {
		Comparator<File> cmp_file_suffix = generate_file_comparator_by_suffix();
		
		ArrayList<ArrayList<StringBuilder>> lines_all_inf = new ArrayList<>();
		for (int p = 0; p < Test_Common_Static_Vars.inf_modelled.length; p++) {
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
					//System.out.printf("qsub %s.pbs\n", singleResultSet.getName());
					completedSet = false;
				}
			}
			
			if(!completedSet) {
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
					for (int p = 0; p < Test_Common_Static_Vars.inf_modelled.length; p++) {
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

			PrintWriter[] pWriters_num_infect = new PrintWriter[Test_Common_Static_Vars.inf_modelled.length];
			for (int p = 0; p < Test_Common_Static_Vars.inf_modelled.length; p++) {
				pWriters_num_infect[p] = new PrintWriter(new File(output_dir,
						String.format("Inf_%d_num_of_infected.csv", Test_Common_Static_Vars.inf_modelled[p])));
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

	public static void extract_infection_history_to_csv(File scenario_dir, 
			HashMap<Integer, String[]> indiv_map_by_cmap,
			int switchTime, int[] inf_modelled)
			throws IOException, FileNotFoundException {

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
		for (int p = 0; p < Test_Common_Static_Vars.inf_modelled.length; p++) {
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

}
