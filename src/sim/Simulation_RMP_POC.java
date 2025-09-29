package sim;

import java.util.Properties;

public class Simulation_RMP_POC extends Simulation_MetaPop {
	
	
	@Override
	public Abstract_Runnable_ClusterModel_Transmission generateDefaultRunnable(long cMap_seed, long sim_seed,
			Properties loadProperties) {
		Runnable_MetaPopulation_Transmission_RMP_MultiInfection run_trans = new Runnable_MetaPopulation_Transmission_RMP_MultiInfection(
				cMap_seed, sim_seed, loadedProperties);

		return run_trans;
	}


}
