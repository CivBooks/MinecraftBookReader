import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.UUID;

import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JPanel;

import org.json.JSONException;
import org.json.JSONObject;

import Anvil.NBTTagCompound;

import java.awt.*;


public class Main {

	static ArrayList<Integer> bookHashes = new ArrayList<Integer>();
	
	public static void main(String[] args) throws IOException
	{
		readBooksAnvil();
		readPlayerData();
		
		/**
		 * GUI TO-DO
		 * Read saves folder, dropdown menu for each world
		 * books + signs in one app
		 * sign filter option. Lifts, [private], empty etc.
		 * Book author / title filter
		 * progress bar
		 * SCAN ITEM FRAME AND FURNACES
		 * ender chests
		 * shulker chests
		 * item frames
		 * remove colour tags and such
		 */
	}
	
	public void displayGUI()
	{
		File  minecraftDir = new File((String)(System.getenv("APPDATA") +  "\\.minecraft\\saves"));
		
		Choice test = new Choice();
		
		for (int i = 0; i < minecraftDir.listFiles().length; i++)
		{
			if (minecraftDir.listFiles()[i].isDirectory())
			{
				File worldFolder = minecraftDir.listFiles()[i];
				Path leveldat = Paths.get(worldFolder.getAbsolutePath() + "\\level.dat");
						
				
				if (Files.exists(leveldat))
					test.add(worldFolder.getName());
			}
		}
		
		Frame frame = new Frame();
		frame.setSize(new Dimension(500,400));
		frame.setLayout(new FlowLayout());
		frame.setTitle("Test");
		frame.setResizable(false);
		frame.setVisible(true);
		
		Button btn = new Button("Output Signs");
		btn.setLocation(1, 10);
		btn.setSize(50,50);
		
		Button btn2 = new Button("Output books");
		btn.setLocation(1, 10);
		btn.setSize(50,50);
		
		frame.add(btn);
		frame.add(btn2);
		frame.add(test);
	}
	
	public static void readPlayerData() throws IOException
	{
		File folder = new File("playerdata");
		File[] listOfFiles = folder.listFiles();
		File output = new File("playerdataOutput.txt");
		BufferedWriter writer = new BufferedWriter(new FileWriter(output));
		
		//Loop through player .dat files
		for (int i = 0; i < listOfFiles.length; i++)
		{	
			System.out.println(i + " / " + listOfFiles.length);
			FileInputStream fileinputstream = new FileInputStream(listOfFiles[i]);
			Anvil.NBTTagCompound playerCompound = Anvil.CompressedStreamTools.readCompressed(fileinputstream);
			Anvil.NBTTagList playerInventory = playerCompound.getTagList("Inventory", 10);
						
			for (int n = 0; n < playerInventory.tagCount(); n ++)
			{
				Anvil.NBTTagCompound item = (Anvil.NBTTagCompound) playerInventory.getCompoundTagAt(n);
				String bookInfo = ("------------------------------------Inventory of player " + listOfFiles[i].getName() + "------------------------------------" );
				parseItem(item, writer, bookInfo);
			}
			
			Anvil.NBTTagList enderInventory = playerCompound.getTagList("EnderItems", 10);
			
			for (int e = 0; e < enderInventory.tagCount(); e ++)
			{
				Anvil.NBTTagCompound item = (Anvil.NBTTagCompound) playerInventory.getCompoundTagAt(e);
				String bookInfo = ("------------------------------------Ender Chest of player " + listOfFiles[i].getName() + "------------------------------------" );
				parseItem(item, writer, bookInfo);
			}
			
			//MCR.NBTTagCompound nbttagcompound = MCR.CompressedStreamTools.func_1138_a(fileinputstream);
		}
		writer.close();
	}
	
	public static void readBooksAnvil() throws IOException
	{
		File folder = new File("region");
		File[] listOfFiles = folder.listFiles();
		File output = new File("bookOutput.txt");
		BufferedWriter writer = new BufferedWriter(new FileWriter(output));
		
		for (int f = 0; f < listOfFiles.length; f++)
		{
			for (int x = 0; x < 32; x++)
			{
				for (int z = 0; z < 32; z++)
				{
					DataInputStream dataInputStream  = MCR.RegionFileCache.getChunkInputStream(listOfFiles[f], x, z); 
					//Stops a null pointer exception when there is no chunk in the .mcr
					if (dataInputStream == null) 
						continue;
					Anvil. NBTTagCompound nbttagcompund = new Anvil.NBTTagCompound();
				    nbttagcompund = Anvil.CompressedStreamTools.read(dataInputStream);
				   
				    Anvil.NBTTagCompound nbttagcompund2 = new Anvil.NBTTagCompound();
				    nbttagcompund2 = nbttagcompund.getCompoundTag("Level");
				       
				    Anvil.NBTTagList tileEntities = nbttagcompund2.getTagList("TileEntities", 10);
	
				    for (int i = 0; i < tileEntities.tagCount(); i ++)
				    {
				    	Anvil.NBTTagCompound tileEntity = (Anvil.NBTTagCompound) tileEntities.getCompoundTagAt(i);
				    	{
				    		if (tileEntity.hasKey("id")) //If it is an item
				    		{
				    			Anvil.NBTTagList chestItems = tileEntity.getTagList("Items", 10);
				    			for (int n = 0; n < chestItems.tagCount(); n++)
				    			{
				    				Anvil.NBTTagCompound item = chestItems.getCompoundTagAt(n);
			    					String bookInfo = ("------------------------------------Chunk [" + x + ", " + z + "] Inside " + tileEntity.getString("id") + " at (" +  tileEntity.getInteger("x") + " " + tileEntity.getInteger("y") + " " + tileEntity.getInteger("z") + ") " + listOfFiles[f].getName() + "------------------------------------" );
			    					parseItem(item, writer, bookInfo);
				    			}
				    		}
				    	}
				    }
				    
				    Anvil.NBTTagList entities = nbttagcompund2.getTagList("Entities", 10);
				    
				    for (int i = 0; i < entities.tagCount(); i ++)
				    {
				    	Anvil.NBTTagCompound entity = (Anvil.NBTTagCompound) entities.getCompoundTagAt(i);
				    	{
				    		//Donkey, llama etc.
				    		if (entity.hasKey("Items"))
				    		{
				    			Anvil.NBTTagList entityItems = entity.getTagList("Items", 10);
				    			Anvil.NBTTagList entityPos = entity.getTagList("Pos", 6);
				    			
				    			int xPos = (int) Double.parseDouble(entityPos.getStringTagAt(0));
				    			int yPos = (int) Double.parseDouble(entityPos.getStringTagAt(1));
				    			int zPos = (int) Double.parseDouble(entityPos.getStringTagAt(2));
				    			
				    			for (int n = 0; n < entityItems.tagCount(); n++)
				    			{
				    				Anvil.NBTTagCompound item = entityItems.getCompoundTagAt(n);
			    					String bookInfo = ("------------------------------------Chunk [" + x + ", " + z + "] On entity at (" + xPos + " " + yPos + " " + zPos + ") " + listOfFiles[f].getName() + "------------------------------------" );
			    					parseItem(item, writer, bookInfo);
				    			}
				    		}
				    		//Item frame or item on the ground
				    		if (entity.hasKey("Item"))
				    		{
				    			Anvil.NBTTagCompound item = entity.getCompoundTag("Item");
				    			Anvil.NBTTagList entityPos = entity.getTagList("Pos", 6);
				    			
				    			int xPos = (int) Double.parseDouble(entityPos.getStringTagAt(0));
				    			int yPos = (int) Double.parseDouble(entityPos.getStringTagAt(1));
				    			int zPos = (int) Double.parseDouble(entityPos.getStringTagAt(2));
				    			
		    					String bookInfo = ("------------------------------------Chunk [" + x + ", " + z + "] On ground or item frame at at (" + xPos + " " + yPos + " " + zPos + ") " + listOfFiles[f].getName() + "--------------------------------" );

				    			parseItem(item, writer, bookInfo);
					    		//System.out.println(item);
				    		}
				    	}
				    }
				}
			}	
		}
		writer.close();
	}
	
	public static void parseItem(Anvil.NBTTagCompound item, BufferedWriter writer, String bookInfo) throws IOException
	{
		if (item.getString("id").equals("minecraft:written_book") || item.getShort("id") == 387)
		{
			readWrittenBook(item, writer, bookInfo);
		}
		if (item.getString("id").equals("minecraft:writable_book") || item.getShort("id") == 386)
		{
			readWritableBook(item, writer, bookInfo);
		}
		if (item.getString("id").contains("shulker_box") || (item.getShort("id") >= 219 && item.getShort("id") <= 234))
		{
			Anvil.NBTTagCompound shelkerCompound = item.getCompoundTag("tag");
			Anvil.NBTTagCompound shelkerCompound2 = shelkerCompound.getCompoundTag("BlockEntityTag");
			Anvil.NBTTagList shelkerContents = shelkerCompound2.getTagList("Items", 10);
			for (int i = 0; i < shelkerContents.tagCount(); i++)
			{
				Anvil.NBTTagCompound shelkerItem = shelkerContents.getCompoundTagAt(i);
				parseItem(shelkerItem, writer, bookInfo);
			}	
		}
	}
	
	private static void readWrittenBook(Anvil.NBTTagCompound item, BufferedWriter writer, String bookInfo) throws IOException
	{
		Anvil.NBTTagCompound tag = item.getCompoundTag("tag");
		Anvil.NBTTagList pages = tag.getTagList("pages", 8);
		
		if (pages.tagCount() == 0)
			return;
		if (bookHashes.contains(pages.hashCode()))
			return;
		else
			bookHashes.add(pages.hashCode());
		
		String author = tag.getString("author");
		String title = tag.getString("title");

		writer.newLine();
		writer.write(bookInfo);
		writer.newLine();
		writer.write("\tTitle: " + title);
		writer.newLine();
		writer.write("\tAuthor: " + author );
		writer.newLine();
		writer.write("\tType: Written");
		writer.newLine();
		writer.newLine();
		for (int pc = 0; pc < pages.tagCount(); pc++)
		{
			JSONObject pageJSON = null;
			String pageText = pages.getStringTagAt(pc);
			
			if (pageText.startsWith("{")) //If valid JSON
			{
				try { pageJSON = new JSONObject(pageText); }
				catch(JSONException e) { pageJSON = null; }
			}
			
			writer.write("Page " + pc + ": ");
			
			//IF VALID JSON
			if (pageJSON != null)
			{
				if (pageJSON.has("extra"))
				{
					
    				for (int h = 0; h < pageJSON.getJSONArray("extra").length(); h++)
    				{
    					if (pageJSON.getJSONArray("extra").get(h) instanceof String)
    						writer.write(pageJSON.getJSONArray("extra").get(0).toString());
    					else
    					{
    						JSONObject temp = (JSONObject) pageJSON.getJSONArray("extra").get(h);
    						writer.write(temp.get("text").toString());
    					}				    			
    				}
				}
				else if (pageJSON.has("text"))
				{
					writer.write(pageJSON.getString("text"));
				}
			}
			else
			{
				writer.write(pages.getStringTagAt(pc));
			}
			
			writer.newLine();
		}
	}
	
	private static void readWritableBook(Anvil.NBTTagCompound item, BufferedWriter writer, String bookInfo) throws IOException
	{
		Anvil.NBTTagCompound tag = item.getCompoundTag("tag");
		Anvil.NBTTagList pages = tag.getTagList("pages", 8);
		if (pages.tagCount() == 0)
			return;
		if (bookHashes.contains(pages.hashCode()))
			return;
		else
			bookHashes.add(pages.hashCode());
		writer.newLine();
		writer.write(bookInfo);
		writer.newLine();
		writer.write("\tType: Writable");
		writer.newLine();
		writer.newLine();
		for (int pc = 0; pc < pages.tagCount(); pc++)
		{
			writer.write("Page " + pc + ": " + pages.getStringTagAt(pc));
			writer.newLine();
		}
	}
	
}
