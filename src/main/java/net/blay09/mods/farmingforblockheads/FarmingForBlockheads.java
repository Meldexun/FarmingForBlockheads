package net.blay09.mods.farmingforblockheads;

import net.blay09.mods.farmingforblockheads.api.FarmingForBlockheadsAPI;
import net.blay09.mods.farmingforblockheads.block.ModBlocks;
import net.blay09.mods.farmingforblockheads.compat.Compat;
import net.blay09.mods.farmingforblockheads.compat.VanillaAddon;
import net.blay09.mods.farmingforblockheads.entity.EntityMerchant;
import net.blay09.mods.farmingforblockheads.item.ModItems;
import net.blay09.mods.farmingforblockheads.network.GuiHandler;
import net.blay09.mods.farmingforblockheads.network.NetworkHandler;
import net.blay09.mods.farmingforblockheads.registry.AbstractRegistry;
import net.blay09.mods.farmingforblockheads.registry.MarketRegistry;
import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLInterModComms;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.Optional;

@Mod(modid = FarmingForBlockheads.MOD_ID, name = "Farming for Blockheads", dependencies = "after:mousetweaks[2.8,);after:forestry;after:agricraft", acceptedMinecraftVersions = "[1.12]")
@Mod.EventBusSubscriber
public class FarmingForBlockheads {

	public static final String MOD_ID = "farmingforblockheads";

	@Mod.Instance(MOD_ID)
	public static FarmingForBlockheads instance;

	@SidedProxy(clientSide = "net.blay09.mods.farmingforblockheads.client.ClientProxy", serverSide = "net.blay09.mods.farmingforblockheads.CommonProxy")
	public static CommonProxy proxy;

	public static Logger logger;

	public static final CreativeTabs creativeTab = new CreativeTabs(MOD_ID) {
		@Override
		public ItemStack getTabIconItem() {
			return new ItemStack(ModBlocks.market);
		}
	};

	public static File configDir;


	@Mod.EventHandler
	public void preInit(FMLPreInitializationEvent event) {
		logger = event.getModLog();
		configDir = new File(event.getModConfigurationDirectory(), "FarmingForBlockheads");
		if (!configDir.exists() && !configDir.mkdirs()) {
			throw new RuntimeException("Couldn't create Farming for Blockheads configuration directory");
		}

		FarmingForBlockheadsAPI.__setupAPI(new InternalMethodsImpl());
		final ResourceLocation CATEGORY_ICONS = new ResourceLocation(FarmingForBlockheads.MOD_ID, "textures/gui/market.png");
		FarmingForBlockheadsAPI.registerMarketCategory(new ResourceLocation(MOD_ID, "seeds"), "gui.farmingforblockheads:market.tooltip_seeds", CATEGORY_ICONS, 196, 14, 10);
		FarmingForBlockheadsAPI.registerMarketCategory(new ResourceLocation(MOD_ID, "saplings"), "gui.farmingforblockheads:market.tooltip_saplings", CATEGORY_ICONS, 196 + 20, 14, 20);
		FarmingForBlockheadsAPI.registerMarketCategory(new ResourceLocation(MOD_ID, "flowers"), "gui.farmingforblockheads:market.tooltip_flowers", CATEGORY_ICONS, 176, 74, 30);
		FarmingForBlockheadsAPI.registerMarketCategory(new ResourceLocation(MOD_ID, "other"), "gui.farmingforblockheads:market.tooltip_other", CATEGORY_ICONS, 196 + 40, 14, 40);

		ModBlocks.registerTileEntities();

		MinecraftForge.EVENT_BUS.register(new FarmlandHandler());
		MinecraftForge.EVENT_BUS.register(new ZombieKillMerchantHandler());
	}

	@Mod.EventHandler
	public void init(FMLInitializationEvent event) {
		ModConfig.validate();

		NetworkHandler.init();
		NetworkRegistry.INSTANCE.registerGuiHandler(this, new GuiHandler());

		new VanillaAddon();
		buildSoftDependProxy(Compat.HARVESTCRAFT, "net.blay09.mods.farmingforblockheads.compat.HarvestcraftAddon");
		buildSoftDependProxy(Compat.FORESTRY, "net.blay09.mods.farmingforblockheads.compat.ForestryAddon");
		buildSoftDependProxy(Compat.AGRICRAFT, "net.blay09.mods.farmingforblockheads.compat.AgriCraftAddon");
		buildSoftDependProxy(Compat.BIOMESOPLENTY, "net.blay09.mods.farmingforblockheads.compat.BiomesOPlentyAddon");
		buildSoftDependProxy(Compat.NATURA, "net.blay09.mods.farmingforblockheads.compat.NaturaAddon");
		buildSoftDependProxy(Compat.TERRAQUEOUS, "net.blay09.mods.farmingforblockheads.compat.TerraqueousAddon");

		MarketRegistry.INSTANCE.load(configDir);
	}

	@Mod.EventHandler
	public void imc(FMLInterModComms.IMCEvent event) {
		IMCHandler.handleIMCMessage(event);
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
		ModBlocks.register(event.getRegistry());
	}

	@SubscribeEvent
	public static void registerItems(RegistryEvent.Register<Item> event) {
		ModBlocks.registerItemBlocks(event.getRegistry());
		ModItems.register(event.getRegistry());
	}

	@SubscribeEvent
	public static void registerSounds(RegistryEvent.Register<SoundEvent> event) {
		ModSounds.register(event.getRegistry());
	}

	@SubscribeEvent
	public static void registerEntities(RegistryEvent.Register<EntityEntry> event) {
		EntityRegistry.registerModEntity(new ResourceLocation(MOD_ID + ":merchant"), EntityMerchant.class, "merchant", 0, instance, 64, 3, true);
	}

	@SubscribeEvent
	public static void registerModels(ModelRegistryEvent event) {
		ModBlocks.registerModels();
		ModItems.registerModels();
		proxy.registerModels();
	}

	@SubscribeEvent
	public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
		if (AbstractRegistry.registryErrors.size() > 0) {
			event.player.sendStatusMessage(new TextComponentString(TextFormatting.RED + "There were errors loading the Farming for Blockheads registries:"), false);
			TextFormatting lastFormatting = TextFormatting.WHITE;
			for (String error : AbstractRegistry.registryErrors) {
				event.player.sendStatusMessage(new TextComponentString(lastFormatting + "* " + error), false);
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
