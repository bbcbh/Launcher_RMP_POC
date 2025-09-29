package sim;

import java.io.IOException;
import java.util.Properties;

public class Simulation_RMP_POC extends Simulation_MetaPop {
	
	
	public static void main(String[] args) throws IOException, InterruptedException {
		final String USAGE_INFO = String.format(
				"Usage: java %s PROP_FILE_DIRECTORY " + "<-export_skip_backup> <-printProgress> <-seedMap=SEED_MAP>\n", 
		Simulation_RMP_POC.class.getName());		
		
		if (args.length < 1) {
			System.out.println(USAGE_INFO);
			System.exit(0);
		}else {
			Simulation_ClusterModelTransmission.launch(args, new Simulation_MetaPop());
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
