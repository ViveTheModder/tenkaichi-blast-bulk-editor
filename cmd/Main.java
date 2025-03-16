package cmd;
//Tenkaichi Blast Bulk Editor by ViveTheModder
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Scanner;

public class Main 
{
	static boolean bt2Mode=false, wiiMode=false;
	
	private static boolean isCharaCostumePak(File pakRef) throws IOException
	{
		wiiMode=false; //treat every PAK as if it is in Little Endian
		RandomAccessFile pak = new RandomAccessFile(pakRef,"r");
		int numPakContents = LittleEndian.getInt(pak.readInt());
		if (numPakContents<0) //prevent negative seek offset
		{
			wiiMode=true; //set endian to Big Endian
			numPakContents = LittleEndian.getInt(pak.readInt());
		}
		if (numPakContents==252) bt2Mode=false;
		else if (numPakContents==250) bt2Mode=true;
		
		pak.seek((numPakContents+1)*4);
		int fileSize = LittleEndian.getInt(pak.readInt());
		int actualFileSize = (int) pak.length();
		pak.close();
		if (fileSize==actualFileSize && numPakContents>=250 && numPakContents<=252) return true;
		return false;
	}
	private static void multiplyBlastDmg(File pakRef, int blastId, double coefficient, boolean isMultiplier) throws IOException
	{
		final int[] BLAST_1_POS = {72,132}, BLAST_2_POS = {12,388};
		int gameIndex=1, offset=100, numEdits=4;
		if (bt2Mode) gameIndex=0;
		if (blastId==2) 
		{
			offset=96; 
			numEdits=6;
		}

		RandomAccessFile pak = new RandomAccessFile(pakRef,"rw");
		pak.seek(offset);
		int pos = LittleEndian.getInt(pak.readInt());
		if (blastId==2) pak.seek(pos+BLAST_2_POS[gameIndex]);
		else pak.seek(pos+BLAST_1_POS[gameIndex]);
		
		for (int i=0; i<numEdits; i++)
		{
			int dmg = LittleEndian.getInt(pak.readInt());
			int newDmg = dmg;
			if (isMultiplier) newDmg*=coefficient;
			else newDmg+=coefficient;
			
			pak.seek(pak.getFilePointer()-4);
			pak.writeInt(LittleEndian.getInt(newDmg));
		}
		pak.close();
	}
	public static void main(String[] args) 
	{
		boolean isMultiplier=false;
		boolean[] validArgs = {true,true,true};
		double coefficient=-1;
		int blastId=0;
		File src=null;
		File[] pakFiles=null;
		Scanner sc = new Scanner(System.in);
		String helpMsg = "Valid usage: java -jar bt-blast-bulk-editor.jar [arg1] [arg2] [arg3]\n"
		+ "* arg1 -> Blast ID (either 1 or 2),\n* arg2 -> Coefficient,\n* arg3 -> Operation (A for Addition, M for Multiplication).";
		
		if (args.length==3)
		{
			//validate blast ID
			if (args[0].matches("[1-2]+") && args[0].length()==1) blastId = Integer.parseInt(args[0]);
			else 
			{
				System.out.println("Invalid Blast ID!");
				validArgs[0]=false;
			}
			//validate coefficient
			if (args[1].matches("^[+]?(([1-9]\\d*)|0)(\\.\\d+)?")) coefficient = Double.parseDouble(args[1]);
			else 
			{
				System.out.println("Invalid coefficient format, positive coefficients only!");
				validArgs[1]=false;
			}
			//validate operation
			if (args[2].equals("M")) isMultiplier=true;
			else if (args[2].equals("A")) isMultiplier=false;
			else
			{
				System.out.println("Invalid operation! It should be either M (multiplication) or A (addition).");
				validArgs[2]=false;
			}
			
			//only proceed if all 3 args are valid
			if (validArgs[0] && validArgs[1] && validArgs[2]) 
			{
				while (src==null)
				{
					System.out.println("Enter a valid path to a folder containing character costume files:");
					String path = sc.nextLine();
					File temp = new File(path);
					if (temp.isDirectory())
					{
						pakFiles = temp.listFiles(new FilenameFilter()
						{
							@Override
							public boolean accept(File dir, String name) 
							{
								String nameLower = name.toLowerCase();
								return nameLower.endsWith("p.pak") || nameLower.endsWith("p_dmg.pak"); 
							}
						});
						if (pakFiles!=null && pakFiles.length!=0) src=temp; 
					}
				}
				sc.close();
				
				long start = System.currentTimeMillis();
				for (File pak: pakFiles)
				{
					try 
					{
						if (isCharaCostumePak(pak)) 
						{
							System.out.println("Overwriting "+pak.getName()+"'s Blast "+blastId+" damage...");
							multiplyBlastDmg(pak, blastId, coefficient, isMultiplier);
						}
					} 
					catch (IOException e) 
					{
						e.printStackTrace();
					}
				}
				long finish = System.currentTimeMillis();
				double time = (finish-start)/1000.0;
				System.out.println("Time: "+time+" s.");
			}
			else System.out.println(helpMsg);
		}
		else
		{
			String msg = "Not enough arguments provided!\n";
			if (args.length==0) msg = msg.replace("t enough", "");
			System.out.println(msg+helpMsg);
		}
	}
}