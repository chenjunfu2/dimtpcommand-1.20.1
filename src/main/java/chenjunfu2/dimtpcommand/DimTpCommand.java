package chenjunfu2.dimtpcommand;

import chenjunfu2.dimtpcommand.mixin.TeleportCommandAccessor;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.DimensionArgumentType;
import net.minecraft.command.argument.PosArgument;
import net.minecraft.command.argument.RotationArgumentType;
import net.minecraft.command.argument.Vec3ArgumentType;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

public class DimTpCommand implements ModInitializer {
	public static final String MOD_ID = "dimtpcommand";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize()
	{
		CommandRegistrationCallback.EVENT.register(DimTpCommand::registerCommands);
	}
	
	private static void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess commandRegistryAccess, CommandManager.RegistrationEnvironment registrationEnvironment)
	{
		var command = CommandManager
			.literal("dimtp")
        	//.requires(source -> source.hasPermissionLevel(2))
        	.then(CommandManager.argument("dimension", DimensionArgumentType.dimension())
        	    .then(CommandManager.argument("location", Vec3ArgumentType.vec3())
					.executes(context -> execute(context, false))
					.then(CommandManager.argument("rotation", RotationArgumentType.rotation())
        	        	.executes(context -> execute(context, true))
        	    	)
				)
        	);
		
		dispatcher.register(command);
	}
	
	private static String formatFloat(double d) {
        return String.format(Locale.ROOT, "%f", d);
    }
	
	private static int execute(CommandContext<ServerCommandSource> context, boolean hasRetation) throws CommandSyntaxException
	{
		ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();

        if (player == null)
		{
            source.sendError(Text.literal("Only player can use this command."));
            return 0;
        }
		
		ServerWorld targetWorld = DimensionArgumentType.getDimensionArgument(context, "dimension");
		PosArgument location = Vec3ArgumentType.getPosArgument(context, "location");
		PosArgument rotation = hasRetation ? RotationArgumentType.getRotation(context, "rotation") : null;
		return TeleportCommandAccessor.execute(source, Collections.singleton(player), targetWorld, location, rotation, null);
	}
}
