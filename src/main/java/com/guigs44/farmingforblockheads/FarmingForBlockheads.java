package com.guigs44.farmingforblockheads;

import com.guigs44.farmingforblockheads.block.BlockMarket;
import com.guigs44.farmingforblockheads.block.ModBlocks;
import com.guigs44.farmingforblockheads.compat.Compat;
import com.guigs44.farmingforblockheads.compat.VanillaAddon;
import com.guigs44.farmingforblockheads.entity.EntityMerchant;
import com.guigs44.farmingforblockheads.network.GuiHandler;
import com.guigs44.farmingforblockheads.network.NetworkHandler;
import com.guigs44.farmingforblockheads.registry.AbstractRegistry;
import com.guigs44.farmingforblockheads.registry.MarketRegistry;
import com.guigs44.farmingforblockheads.tile.TileMarket;
import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import net.minecraftforge.fml.common.registry.GameRegistry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.Optional;

@Mod(modid = FarmingForBlockheads.MOD_ID, name = "Farming for Blockheads", dependencies = "after:mousetweaks[2.8,);after:forestry;after:agricraft")
@Mod.EventBusSubscriber
public class FarmingForBlockheads {

	public static final String MOD_ID = "farmingforblockheads";

	@Mod.Instance(MOD_ID)
	public static FarmingForBlockheads instance;

	@SidedProxy(clientSide = "com.guigs44.farmingforblockheads.client.ClientProxy", serverSide = "com.guigs44.farmingforblockheads.CommonProxy")
	public static CommonProxy proxy;

	public static final Logger logger = LogManager.getLogger();

	public static final CreativeTabs creativeTab = new CreativeTabs(MOD_ID) {
		@Override
		public Item getTabIconItem() {
			//noinspection ConstantConditions
			return Item.getItemFromBlock(ModBlocks.market);
		}
	};

	public static File configDir;
	private Configuration config;

	@Mod.EventHandler
	public void preInit(FMLPreInitializationEvent event) {
		configDir = new File(event.getModConfigurationDirectory(), "FarmingForBlockheads");
		if (!configDir.exists() && !configDir.mkdirs()) {
			throw new RuntimeException("Couldn't create Farming for Blockheads configuration directory");
		}
		config = new Configuration(new File(configDir, "FarmingForBlockheads.cfg"));
		config.load();
		ModConfig.preInit(config);

		GameRegistry.registerTileEntity(TileMarket.class, MOD_ID + ":market");

		proxy.preInit();

		if(config.hasChanged()) {
			config.save();
		}
	}

	@Mod.EventHandler
	public void init(FMLInitializationEvent event) {
		NetworkHandler.init();
		NetworkRegistry.INSTANCE.registerGuiHandler(this, new GuiHandler());

		new VanillaAddon();
		buildSoftDependProxy(Compat.HARVESTCRAFT, "com.guigs44.farmingforblockheads.compat.HarvestcraftAddon");
		buildSoftDependProxy(Compat.FORESTRY, "com.guigs44.farmingforblockheads.compat.ForestryAddon");
		buildSoftDependProxy(Compat.AGRICRAFT, "com.guigs44.farmingforblockheads.compat.AgriCraftAddon");
		buildSoftDependProxy(Compat.BIOMESOPLENTY, "com.guigs44.farmingforblockheads.compat.BiomesOPlentyAddon");
		buildSoftDependProxy(Compat.NATURA, "com.guigs44.farmingforblockheads.compat.NaturaAddon");

		ModRecipes.init();
		MarketRegistry.INSTANCE.load(configDir);

		EntityRegistry.registerModEntity(EntityMerchant.class, "merchant", 0, this, 64, 3, true);

		proxy.init();
	}

	@Mod.EventHandler
	public void postInit(FMLPostInitializationEvent event) {
	}

	@Mod.EventHandler
	public void serverStarting(FMLServerStartingEvent event) {
		event.registerServerCommand(new CommandFarmingForBlockheads());
	}

	@SubscribeEvent
	public static void registerBlocks(RegistryEvent.Register<Block> event) {
		event.getRegistry().registerAll((ModBlocks.market = new BlockMarket()));
	}

	@SubscribeEvent
	public static void registerItems(RegistryEvent.Register<Item> event) {
		event.getRegistry().registerAll(new ItemBlock(ModBlocks.market).setRegistryName(ModBlocks.market.getRegistryName()));
	}

	@SubscribeEvent
	public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
		if (AbstractRegistry.registryErrors.size() > 0) {
			event.player.addChatComponentMessage(new TextComponentString(TextFormatting.RED + "There were errors loading the Farming for Blockheads registries:"));
			TextFormatting lastFormatting = TextFormatting.WHITE;
			for (String error : AbstractRegistry.registryErrors) {
				event.player.addChatMessage(new TextComponentString(lastFormatting + "* " + error));
				lastFormatting = lastFormatting == TextFormatting.GRAY ? TextFormatting.WHITE : TextFormatting.GRAY;
			}
		}
	}

	private Optional<?> buildSoftDependProxy(String modId, String className) {
		if (Loader.isModLoaded(modId)) {
			try {
				Class<?> clz = Class.forName(className, true, Loader.instance().getModClassLoader());
				return Optional.ofNullable(clz.newInstance());
			} catch (Exception e) {
				return Optional.empty();
			}
		}
		return Optional.empty();
	}

}