package gov.ca.water.io.HDF5;

import ncsa.hdf.hdf5lib.H5;
import ncsa.hdf.hdf5lib.HDF5Constants;
import ncsa.hdf.hdf5lib.exceptions.HDF5LibraryException;

import java.util.*;

public class HDF5Util {
	
	public static int locateFile(String fileName){
		
		try {
			return H5.H5Fopen(fileName, HDF5Constants.H5F_ACC_TRUNC, HDF5Constants.H5P_DEFAULT);
		} catch (Exception e) {
		
			try {
				return H5.H5Fcreate(fileName, HDF5Constants.H5F_ACC_TRUNC,
							HDF5Constants.H5P_DEFAULT, HDF5Constants.H5P_DEFAULT);
			} catch (HDF5LibraryException e1) {
				e1.printStackTrace();
				return -1;
			} catch (NullPointerException e1) {
				e1.printStackTrace();
				return -1;
			}
		}
	}
	
	public static int locateGroup(int id, String group){

		try {
			int gid = H5.H5Gopen(id, group, 0);
			if (gid<0){
				gid=H5.H5Gcreate(id, group,
				        HDF5Constants.H5P_DEFAULT, HDF5Constants.H5P_DEFAULT, HDF5Constants.H5P_DEFAULT);
			}
			return gid;
		} catch (HDF5LibraryException e) {
			try {
				int gid = H5.H5Gcreate(id, group,
				        HDF5Constants.H5P_DEFAULT, HDF5Constants.H5P_DEFAULT, HDF5Constants.H5P_DEFAULT);
				return gid;
			} catch (HDF5LibraryException e1) {
				e1.printStackTrace();
				return -1;
			} catch (NullPointerException e1) {
				e1.printStackTrace();
				return -1;
			}
		} catch (NullPointerException e) {
			try {
				int gid = H5.H5Gcreate(id, group,
				        HDF5Constants.H5P_DEFAULT, HDF5Constants.H5P_DEFAULT, HDF5Constants.H5P_DEFAULT);
				return gid;
			} catch (HDF5LibraryException e1) {
				e1.printStackTrace();
				return -1;
			} catch (NullPointerException e1) {
				e1.printStackTrace();
				return -1;
			}
		}	
	}

	public static void writeStringData(int did, int tid, String[] stringArray, int stringLength){
		try {
			int size =stringArray.length;
			byte[][] write_data =  new byte[size][stringLength];
			for (int indx = 0; indx <size ; indx++) {
				for (int jndx = 0; jndx < stringLength; jndx++) {
					if (jndx < stringArray[indx].length())
						write_data[indx][jndx] = (byte) stringArray[indx].charAt(jndx);
					else
						write_data[indx][jndx] = 0;
				}
			}

			if ((did >= 0) && (tid >= 0))
				H5.H5Dwrite(did, tid, 
				        HDF5Constants.H5S_ALL, HDF5Constants.H5S_ALL, HDF5Constants.H5P_DEFAULT, 
				        write_data);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static String[] readStringData(int did, int tid, int size, int stringLength){
		try {
			byte[] read_data =  new byte[size*stringLength];
			String[] stringData=new String[size];
			
			if ((did >= 0) && (tid >= 0))
				H5.H5Dread(did, tid, 
				        HDF5Constants.H5S_ALL, HDF5Constants.H5S_ALL, HDF5Constants.H5P_DEFAULT, 
				        read_data);
			
			for (int indx = 0; indx <size ; indx++) {
				byte[] buff=new byte[stringLength];
				for (int jndx = 0; jndx < stringLength; jndx++) {
					buff[jndx]=read_data[indx*stringLength+jndx];
				}
				stringData[indx]=(new String(buff)).trim().toLowerCase();
			}
			return stringData;
		}
		catch (Exception e) {
			String[] stringData=new String[0];
			e.printStackTrace();
			return stringData;
		}
	}
	
	public static void writeStringAttr(int aid, int tid, String[] stringArray, int stringLength){
		try {
			int size =stringArray.length;
			byte[][] write_data =  new byte[size][stringLength];
			for (int indx = 0; indx <size ; indx++) {
				for (int jndx = 0; jndx < stringLength; jndx++) {
					if (jndx < stringArray[indx].length())
						write_data[indx][jndx] = (byte) stringArray[indx].charAt(jndx);
					else
						write_data[indx][jndx] = 0;
				}
			}

			if ((aid >= 0) && (tid >= 0))
				H5.H5Awrite(aid, tid, write_data);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static String readStringAttr(int aid, int tid, int stringLength){
		try {
			byte[] readData=new byte[stringLength];
			if ((aid >= 0) && (tid >= 0))
				H5.H5Aread(aid, tid, readData);
			
			String stringAttr=(new String(readData)).toLowerCase().trim();
			return stringAttr;
		}catch (Exception e) {
			e.printStackTrace();
			return "";
		}
	}
	
	public static void writeCycleVariableNames(String[] write_data, int gid, String strCycleI){
		
		String dName="Cycle "+strCycleI+" List";
		
		int size = write_data.length;
		long[] dims = {size};
		
		try {
			int tidName = H5.H5Tcopy(HDF5Constants.H5T_C_S1);
			H5.H5Tset_size(tidName, 256);
				
			int sidList = H5.H5Screate_simple(1, dims, null);
			if (sidList >= 0 && size>0){
				int didList=-1;
				try{
					didList = H5.H5Dopen(gid, dName);
				}catch(Exception e){
					didList = H5.H5Dcreate(gid,
							dName, tidName,
							sidList, HDF5Constants.H5P_DEFAULT, HDF5Constants.H5P_DEFAULT, HDF5Constants.H5P_DEFAULT);
				}
				
				if (didList >= 0){
					HDF5Util.writeStringData(didList, tidName, write_data, 256);
				}
				
	            H5.H5Sclose(sidList);
	            H5.H5Tclose(tidName);
	            H5.H5Dclose(didList);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	public static void writeCycleStaticSv(int index, int size, int gid, double[][] write_data, ArrayList<Integer> currTimeStep){
		String dName="Cycle "+index+" Table";
		long[] dims = {1, size};
		long[] dims1 = {1, size};
		long[] maxDims={HDF5Constants.H5S_UNLIMITED, HDF5Constants.H5S_UNLIMITED};
		long[] chunkDims={1,5};
		
		int sidVs;
		try {				
			int didVs =-1;
			try{
				didVs = H5.H5Dopen(gid, dName);
				Integer currTimestep = currTimeStep.get(index-1);
				dims[0]=currTimestep+1;
				H5.H5Dextend(didVs, dims);
				
				int fsidVs = H5.H5Dget_space (didVs);
				long[] offset1={0,0};
			    offset1[0] = currTimestep;
			    offset1[1] = 0;
			    H5.H5Sselect_hyperslab(fsidVs, HDF5Constants.H5S_SELECT_SET, offset1, null,
			                                  dims1, null); 
			    sidVs = H5.H5Screate_simple (2, dims1, null); 
			    
				H5.H5Dwrite(didVs, HDF5Constants.H5T_NATIVE_DOUBLE, 
					sidVs, fsidVs, HDF5Constants.H5P_DEFAULT, write_data);
				
				H5.H5Sclose(fsidVs);
				H5.H5Dclose(didVs);
				H5.H5Sclose(sidVs);
				
			}catch (Exception e){			
				sidVs = H5.H5Screate_simple(2, dims, maxDims);
				if (sidVs >= 0){
					int cparms = H5.H5Pcreate (HDF5Constants.H5P_DATASET_CREATE);
					H5.H5Pset_chunk ( cparms, 2, chunkDims);
					didVs = H5.H5Dcreate(gid, dName, HDF5Constants.H5T_NATIVE_DOUBLE, sidVs, cparms);
				
					if (didVs >= 0){		
						H5.H5Dextend(didVs, dims);
					
						int fsidVs = H5.H5Dget_space (didVs);
						long[] offset1={0,0};
						offset1[0] = 0;
						offset1[1] = 0;
						H5.H5Sselect_hyperslab(fsidVs, HDF5Constants.H5S_SELECT_SET, offset1, null,
								dims1, null); 
				    
						H5.H5Dwrite(didVs, HDF5Constants.H5T_NATIVE_DOUBLE, 
								sidVs, fsidVs, HDF5Constants.H5P_DEFAULT, write_data);
						
						H5.H5Sclose(fsidVs);
					}
				}
				H5.H5Dclose(didVs);
				H5.H5Sclose(sidVs);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} 
	}

}
