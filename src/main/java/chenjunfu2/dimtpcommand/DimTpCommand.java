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
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        	    .then(CommandManager.argument("x", DoubleArgumentType.doubleArg())
        	        .then(CommandManager.argument("y", DoubleArgumentType.doubleArg())
        	            .then(CommandManager.argument("z", DoubleArgumentType.doubleArg())
        	                .executes(DimTpCommand::execute)
        	            )
        	        )
        	    )
        	);
		
		dispatcher.register(command);
	}
	
	private static String formatFloat(double d) {
        return String.format(Locale.ROOT, "%f", d);
    }
	
	private static int execute(CommandContext<ServerCommandSource> context) throws CommandSyntaxException
	{
		ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();

        if (player == null)
		{
            source.sendError(Text.literal("Only player can use this command."));
            return 0;
        }
		
		ServerWorld targetWorld = DimensionArgumentType.getDimensionArgument(context, "dimension");

        double x = DoubleArgumentType.getDouble(context, "x");
        double y = DoubleArgumentType.getDouble(context, "y");
        double z = DoubleArgumentType.getDouble(context, "z");
		
		Set<PositionFlag> set = EnumSet.of(PositionFlag.X_ROT, PositionFlag.Y_ROT);
		
		TeleportCommandAccessor.teleport(source, player, targetWorld, x, y, z, set, player.getYaw(), player.getPitch(), null);
		source.sendFeedback(() -> Text.translatable("commands.teleport.success.location.single", new Object[]{player.getDisplayName(), formatFloat(x), formatFloat(y), formatFloat(z)}), true);
	
		return 1;
	}
}
