package me.iblitzkriegi.vixio.effects.effGuildManagement;

import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import me.iblitzkriegi.vixio.registration.EffectAnnotation;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.TextChannel;
import org.bukkit.event.Event;

import static me.iblitzkriegi.vixio.effects.EffLogin.bots;

/**
 * Created by Blitz on 11/8/2016.
 */
@EffectAnnotation.Effect(
        name = "SetChannelTopic",
        title = "Set Channel Topic",
        desc = "Set the topic of a Channel in a Guild, bot must have permission to do so",
        syntax = "[discord] set channel [with id] %string% topic to %string% with [bot] %string%",
        example = "SOON"
)
public class EffSetChannelTopic extends Effect{
    Expression<String> vChannel;
    Expression<String> vTopic;
    Expression<String> vBot;
    @Override
    protected void execute(Event e) {
        JDA jda = bots.get(vBot.getSingle(e));
        TextChannel vTc = jda.getTextChannelById(vChannel.getSingle(e));
        vTc.getManager().setTopic(vTopic.getSingle(e)).queue();
    }

    @Override
    public String toString(Event e, boolean b) {
        return getClass().getName();
    }

    @Override
    public boolean init(Expression<?>[] expr, int i, Kleenean kleenean, SkriptParser.ParseResult parseResult) {
        vChannel = (Expression<String>) expr[0];
        vTopic = (Expression<String>) expr[1];
        vBot = (Expression<String>) expr[2];
        return true;
    }
}
