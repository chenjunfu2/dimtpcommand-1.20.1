package chenjunfu2.dimtpcommand.mixin.client;

import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xaero.common.minimap.waypoints.Waypoint;
import xaero.hud.minimap.waypoint.WaypointTeleport;
import xaero.hud.minimap.world.MinimapWorld;
import xaero.hud.path.XaeroPath;
import xaero.map.WorldMap;
import xaero.map.common.config.option.WorldMapProfiledConfigOptions;

@Pseudo
@Mixin(value = WaypointTeleport.class, remap = false)
public abstract class WaypointTeleportMixin {
	private static final Logger LOGGER = LoggerFactory.getLogger("dimtpcommand");
	private static final ThreadLocal<String> DIMENSION_ID = new ThreadLocal<>();
	private static boolean compatibilityWarningLogged;

	@Inject(method = "teleportToWaypoint(Lxaero/common/minimap/waypoints/Waypoint;Lxaero/hud/minimap/world/MinimapWorld;Lnet/minecraft/client/gui/screen/Screen;Z)V", at = @At("HEAD"), require = 0, remap = true)
	private void dimtpcommand$captureDimension(Waypoint waypoint, MinimapWorld world, Screen screen, boolean ignoreHiddenCoordinates, CallbackInfo ci) {
		DIMENSION_ID.set(dimtpcommand$getDimensionId(world));
	}

	@ModifyVariable(
		method = "teleportToWaypoint(Lxaero/common/minimap/waypoints/Waypoint;Lxaero/hud/minimap/world/MinimapWorld;Lnet/minecraft/client/gui/screen/Screen;Z)V",
		at = @At("STORE"),
		slice = @Slice(
			from = @At(value = "INVOKE", target = "Lxaero/hud/minimap/world/container/config/RootConfig;isUsingDefaultTeleportCommand()Z", remap = false),
			to = @At(value = "INVOKE", target = "Ljava/lang/String;isEmpty()Z")
		),
		require = 0,
		remap = true
	)
	private String dimtpcommand$useWorldMapCommandFormat(String originalFormat) {
		if (DIMENSION_ID.get() == null) {
			return originalFormat;
		}

		try {
			if (WorldMap.INSTANCE == null) {
				return originalFormat;
			}

			Object configuredFormat = WorldMap.INSTANCE.getConfigs().getClientConfigManager()
				.getEffective(WorldMapProfiledConfigOptions.DEFAULT_MAP_TELEPORT_DIMENSION_FORMAT);
			return configuredFormat instanceof String format && !format.isBlank() ? format : originalFormat;
		} catch (LinkageError | RuntimeException e) {
			dimtpcommand$logCompatibilityWarning("无法读取世界地图传送模板，将使用 Xaero 原命令。", e);
			return originalFormat;
		}
	}

	@ModifyArg(
		method = "teleportToWaypoint(Lxaero/common/minimap/waypoints/Waypoint;Lxaero/hud/minimap/world/MinimapWorld;Lnet/minecraft/client/gui/screen/Screen;Z)V",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayNetworkHandler;sendCommand(Ljava/lang/String;)Z"),
		index = 0,
		require = 0,
		remap = true
	)
	private String dimtpcommand$replaceDimension(String command) {
		String dimensionId = DIMENSION_ID.get();
		return dimensionId == null ? command : command.replace("{d}", dimensionId);
	}

	@Inject(method = "teleportToWaypoint(Lxaero/common/minimap/waypoints/Waypoint;Lxaero/hud/minimap/world/MinimapWorld;Lnet/minecraft/client/gui/screen/Screen;Z)V", at = @At("RETURN"), require = 0, remap = true)
	private void dimtpcommand$clearDimension(Waypoint waypoint, MinimapWorld world, Screen screen, boolean ignoreHiddenCoordinates, CallbackInfo ci) {
		DIMENSION_ID.remove();
	}

	private String dimtpcommand$getDimensionId(MinimapWorld world) {
		try {
			XaeroPath path = world.getContainer().getPath();
			if (path.getNodeCount() <= 1) {
				return null;
			}

			String directoryName = path.getAtIndex(1).getLastNode();
			var sessionField = WaypointTeleport.class.getDeclaredField("minimapSession");
			sessionField.setAccessible(true);
			Object minimapSession = sessionField.get(this);
			Object dimensionHelper = minimapSession.getClass().getMethod("getDimensionHelper").invoke(minimapSession);
			Object dimensionKey = dimensionHelper.getClass()
				.getMethod("getDimensionKeyForDirectoryName", String.class)
				.invoke(dimensionHelper, directoryName);
			return dimensionKey == null ? null : dimensionKey.getClass().getMethod("getValue").invoke(dimensionKey).toString();
		} catch (ReflectiveOperationException | RuntimeException e) {
			dimtpcommand$logCompatibilityWarning("无法解析 waypoint 目标维度，将使用 Xaero 原命令。", e);
			return null;
		}
	}

	private static void dimtpcommand$logCompatibilityWarning(String message, Throwable cause) {
		if (!compatibilityWarningLogged) {
			compatibilityWarningLogged = true;
			LOGGER.warn(message, cause);
		}
	}
}
