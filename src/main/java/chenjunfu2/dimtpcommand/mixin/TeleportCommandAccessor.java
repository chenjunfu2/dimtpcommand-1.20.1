package chenjunfu2.dimtpcommand.mixin;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.argument.PosArgument;
import net.minecraft.entity.Entity;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.command.TeleportCommand;
import net.minecraft.server.world.ServerWorld;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.Collection;

@Mixin(TeleportCommand.class)
public interface TeleportCommandAccessor
{
	@Invoker("execute")
	static int execute(ServerCommandSource source, Collection<? extends Entity> targets, ServerWorld world, PosArgument location, @Nullable PosArgument rotation, @Nullable TeleportCommand.LookTarget facingLocation) throws CommandSyntaxException
	{
		throw new AssertionError("Mixin invoker");
	}
}