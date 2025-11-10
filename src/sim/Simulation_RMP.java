package sim;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import util.Util_Analyse_RMP;

public class Simulation_RMP extends Simulation_ClusterModelTransmission {

	public static final String PROP_BASEDIR = "PROP_BASEDIR";
	public static final String PROP_LOC_MAP = "PROP_LOC_MAP";
	public static final String PROP_PRELOAD_FILES = "PROP_PRELOAD_FILES";
	public static final String PROP_INDIV_STAT = "PROP_INDIV_STAT";
	public static final String PROP_PARNTER_EXTRA_SOUGHT = "PROP_PARNTER_EXTRA_SOUGHT";
	public static final String PROP_CONTACT_MAP_LOC = "PROP_CONTACT_MAP_LOC";

	public static void main(String[] args) throws IOException, InterruptedException {
		final String USAGE_INFO = String.format(
				"Usage: java %s PROP_FILE_DIRECTORY " + "<-export_skip_backup> <-printProgress> <-seedMap=SEED_MAP>\n"
						+ "  or java %s -analyse PROP_FILE_DIRECTORY SETTING_XML <-simSel=SIM_SEL_XML>",
				Simulation_RMP.class.getName(), Simulation_RMP.class.getName());

		if (args.length < 1) {
			System.out.println(USAGE_INFO);
			System.exit(0);
		} else {

			if (args[0].startsWith("-")) {
				if (args[0].equals("-analyse")) {
					File sce_dir = new File(args[1]);

					// XML setting
					File setting_xml = new File(args[1], args[2]);
					Properties prop = new Properties();
					FileInputStream fin = new FileInputStream(setting_xml);
					prop.loadFromXML(fin);
					fin.close();

					int[] sample_time = (int[]) util.PropValUtils
							.propStrToObject(prop.getProperty(Util_Analyse_RMP.XML_SETTING_SAMPLE_TIME), int[].class);
					HashMap<Long, HashMap<Integer, String[]>> demographic = Util_Analyse_RMP
							.generate_demographic_mapping_from_file(
									new File(prop.getProperty(Util_Analyse_RMP.XML_SETTING_DEMOGRAPHIC_DIR)));
					String[] morbidity_key_arr = prop.getProperty(Util_Analyse_RMP.XML_SETTING_MORBIDITY_KEY_ARR)
							.replaceAll("\\s", "").split(",");

					Map<String, Map<String, Object>> morbidity_setting = new HashMap<>();

					Pattern pattern_morbidity_setting = Pattern
							.compile(Util_Analyse_RMP.XML_SETTING_MORBIDITY_FORMAT.replaceAll("%s", "(.*)"));

					Map<String, Class<?>> classMap = util.Util_Analyse_RMP.CLASSMAP_SETTING;

					for (String key : prop.stringPropertyNames()) {
						Matcher m = pattern_morbidity_setting.matcher(key);
						HashMap<String, Object> entMap = new HashMap<>();
						if (m.matches()) {
							String[] ent_pairs = prop.getProperty(key).split("\n");
							for (String ent_pair : ent_pairs) {
								String[] k_ent = ent_pair.split("=");
								Object val;
								Class<?> cls = classMap.get(k_ent[0]);
								if (cls.equals(Integer.class)) {
									val = Integer.parseInt(k_ent[1]);
								} else {
									val = util.PropValUtils.propStrToObject(k_ent[1], cls);
								}
								entMap.put(k_ent[0], val);
							}
							morbidity_setting.put(m.group(1), entMap);
						}

					}

					// Sim sel_map

					HashMap<String, ArrayList<String>> sim_sel_map = null;
					for (int i = 3; i < args.length; i++) {
						if (args[i].startsWith("-simSel=")) {
							File simSelXML = new File(sce_dir, args[i].split("=")[1]);
							sim_sel_map = util.Util_Analyse_RMP.importSimSelMap(simSelXML);
						}
					}

					Util_Analyse_RMP.extracted_InfectHist(new File[] { sce_dir }, sce_dir, sample_time, sim_sel_map,
							demographic, morbidity_key_arr, morbidity_setting);

				} else {
					System.out.println(USAGE_INFO);
					System.exit(0);
				}

			} else {
				Simulation_ClusterModelTransmission.launch(args, new Simulation_RMP());
			}
		}

	}

	@Override
	public Abstract_Runnable_ClusterModel_Transmission generateDefaultRunnable(long cMap_seed, long sim_seed,
			Properties loadProperties) {
		Runnable_MetaPopulation_Transmission_RMP_MultiInfection run_trans = new Runnable_MetaPopulation_Transmission_RMP_MultiInfection(
				cMap_seed, sim_seed, loadedProperties);

		return run_trans;
	}

}
