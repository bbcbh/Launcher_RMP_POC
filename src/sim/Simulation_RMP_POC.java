package sim;

import java.io.IOException;
import java.util.Properties;

public class Simulation_RMP_POC extends Simulation_ClusterModelTransmission {
	
	public static final String PROP_BASEDIR = "PROP_BASEDIR";
	public static final String PROP_LOC_MAP = "PROP_LOC_MAP";
	public static final String PROP_PRELOAD_FILES = "PROP_PRELOAD_FILES";
	public static final String PROP_INDIV_STAT = "PROP_INDIV_STAT";
	public static final String PROP_PARNTER_EXTRA_SOUGHT = "PROP_PARNTER_EXTRA_SOUGHT";
	public static final String PROP_CONTACT_MAP_LOC = "PROP_CONTACT_MAP_LOC";	
	
	public static void main(String[] args) throws IOException, InterruptedException {
		final String USAGE_INFO = String.format(
				"Usage: java %s PROP_FILE_DIRECTORY " + "<-export_skip_backup> <-printProgress> <-seedMap=SEED_MAP>\n", 
		Simulation_RMP_POC.class.getName());		
		
		if (args.length < 1) {
			System.out.println(USAGE_INFO);
			System.exit(0);
		}else {
			Simulation_ClusterModelTransmission.launch(args, new Simulation_RMP_POC());
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
