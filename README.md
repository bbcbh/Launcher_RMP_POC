# Launcher_RMP_POC

## Overview
Launcher_RMP_POC is a Java-based Individual-Based Model (IBM) designed to explore the potential impact of introducing Point-of-Care (POC) testing on sexually transmitted infection (STI) transmission among Indigenous populations in Australia.
A key feature of this model is its support for four STIs—chlamydia, gonorrhoea, trichomonas, and syphilis—along with mechanisms to calculate associated morbidities.

## Maintainer and developers
* Ben Hui; ORCiD ID: [0000-0002-6567-5821](https://orcid.org/0000-0002-6567-5821)

## Project description
This model extends the [Package_ClusterModel](https://github.com/The-Kirby-Institute/Package_ClusterModel) o simulate the effects of POC testing on STI transmission dynamics. It introduces additional logic to support:
* Delays between diagnosis and treatment
* Calculation of STI-related morbidities

Model parameters are defined in:
* simSpecificSim.prop
* simSpecificSwitch.prop (if present)
* User defined SEED_MAP file (if present)

See [Working directory](#working-directory) for setup details.

## Dependiencies
In addition to Launcher_RMP_POC, the following packages are required:
* [Package_BaseModel](https://github.com/The-Kirby-Institute/Package_BaseModel)
* [Package_ClusterModel](https://github.com/The-Kirby-Institute/Package_ClusterModel)
* [RNG](https://github.com/The-Kirby-Institute/RNG)
* [Package_MetaPopulation](https://github.com/The-Kirby-Institute/Package_MetaPopulation)

## Main classes
* Simulation_RMP 
  Extends _Simulation_ClusterModelTransmission_ to launch simulations with POC support.
* Runnable_MetaPopulation_Transmission_RMP_MultiInfection
  Extends _Runnable_ClusterModel_MultiTransmission_. Implements the model logic for a single simulation and is invoked by _Simulation_RMP_. 

## Setup
The model consists of three main components:

* Network: Partnership network and risk group CSVs
* Executable: JAR files and libraries
* Working Directory: Simulation configuration and output files

These components can be located anywhere, as long as paths are correctly set in simSpecificSim.prop or passed as input argument. For clarity, it's recommended to keep the working directory separate.

### Example Directory Structure
<pre>
Network/
└── <b><i>Partnership network and risk groups CSVs</i></b>
Executable/
├── ClusterModel_lib/
│   └── <b><i>class libraries use by ClusterModel.jar</i></b>
├── RNG.jar
├── Package_BaseModel.jar
├── Package_ClusterModel.jar
├── Package_MetaPopulation.jar
└── Launcher_RMP_POC.jar
WorkingDir/
├── simSpecificSim.prop
├── simSpecificSwitch.prop
└── Seed_List.csv <b><i>for example</i></b>
</pre>

### Quick start 

1. Generate Network Files
   Create the required network, and if required, associated risk groups files, and place them in the Network folder.

2. Download Required Executable and Packages, and place all required JAR files in the Executable folder.

3. Set Up Working Directory, include _simSpecificSim.prop_ and optionally _simSpecificSwitch.prop_

4. (Optional) Prepare Seed List
   Generate a seed list (a CSV file) if required.

5. Configure Simulation Settings
   Edit simSpecificSim.prop to set file paths and simulation parameters.

6. Run the Model
   Execute the model (see [Executable](#executable). 
   
### Executable 
To run the model, use the following Java command (replace items in <b>bold</b> with your specific setup):
<pre>
java -jar Launcher_RMP_POC.jar <b><i>File_Path_Working_Directory</i></b> [-export_skip_backup] [-printProgress] [-seedMap=<b>SEED_MAP</b>]
</pre>
Arguments:
* <b><i>File_Path_Working_Directory</i></b>: (Required) Path to the working directory where the simulation will run.
* -export_skip_backup: (Optional) Skips the creation of backup files for each snapshot.
* -printProgress: (Optional) Displays simulation progress in the console output.
* -seedMap=<b>SEED_MAP</b>: (Optional) Specifies the path (<b>SEED_MAP</b>) to a custom seed map file.

### Working directory
The working directory **must** contain one XML file named simSpecificSim.prop, which defines the simulation settings and default model parameters.

Optionally, the directory may also include:

* simSpecificSwitch.prop: Specifies when and how parameter values change during the simulation.
* One or more seed list CSV files (either in the root or a subfolder of Working directory)


Both simSpecificSwitch.prop and seed list files follow the same format and purpose as those used in the  [Package_ClusterModel](https://github.com/The-Kirby-Institute/Package_ClusterModel). Please refer to its documentation for further details.

#### simSpecificSim.prop
This model extends the _Runnable_ClusterModel_MultiTransmission_ class from the Package_ClusterModel package. As such, most parameter configurations applicable to _Runnable_ClusterModel_MultiTransmission_ remain applicable — especially those related to sexual behavior, STI natural history, and testing.

To incorporate POC testing effects, the field POP_PROP_INIT_PREFIX_21 uses the following format:

<pre>
double{
{GENDER_INCLUDE_INDEX, INF_INCLUDE_INDEX, SITE_INCLUDE_INDEX, RISK_GRP_INCLUDE_INDEX, TESTING_RATE_PARAM...}, ...}
  with 
		TESTING_RATE_PARAM Format:			
			Annual_Coverage
			, DELAY_INF_INC_0, NUM_DELAY_OPTION_0, NUM_DELAY_RANGE_0
			  Prob_Option_0, Prob_Option_1, ... Prob_Treatment_Delay_Range_0... Delay_Range...,
			or
			-1, POS_INF_INCL (or -1 or suspectible), Prob_Option_0_Range_0, Prob_Option_0_Range_1, ... Prob_Retest_Range_0... Retest_Range...,
</pre>

##### Example 
```xml
<entry key="POP_PROP_INIT_PREFIX_21">
[[
48,15,3,1
,0.20
,14,2,2 
,0.5,1
,0.85,1.0,120
,0.97,1.0,120
,1,1,2
,1
,1,1,0,1
]
,[48,15,3,1,-1,15,0.15,1,360]
,[48,15,3,1,-1,-1,0.02,360]
]</entry>
```
Represent the case where:
* 20% annual testing coverage is applied to individuals in risk groups 4 and 5, represented by the bitmask 0b110000 (decimal 48).
* Annual testing targets infections 1, 2, and 3, represented by 0b1110 (decimal 14). Among those tested: 50% experience a treatment delay of less than 120 days, with 85% treated within that timeframe. The remaining 50% are treated with a 97% probability within the same delay window.
* Infection 0 is also tested annually (represented by 0b1 or 1), but no treatment delay is applied.
* For individuals with a positive diagnosis of infections 1–4, 15% are scheduled for retesting within one year.
* For those with a negative diagnosis, 2% are scheduled for retesting within one year.


## Acknowledgments 
This research was produced in whole or part by UNSW Sydney researchers and is subject to the UNSW intellectual property policy. For the purposes of Open Access, the author has applied a Creative Commons Attribution CC-BY public copyright licence to any Author Accepted Manuscript (AAM) version arising from this submission.
