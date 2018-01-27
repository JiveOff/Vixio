package me.iblitzkriegi.vixio.expressions.guild;

import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import me.iblitzkriegi.vixio.Vixio;
import net.dv8tion.jda.core.entities.Category;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.VoiceChannel;
import org.bukkit.event.Event;

import java.util.List;

public class ExprVoiceChannelsOf extends SimpleExpression<VoiceChannel> {
    static {
        Vixio.getInstance().registerExpression(ExprVoiceChannelsOf.class, VoiceChannel.class, ExpressionType.SIMPLE,
                "voice[(-| )]channel[s] of %guild/category%")
                .setName("Voice Channels of")
                .setDesc("Get all of the Voice Channels of a Guild/Category.")
                .setExample(
                        "on guild message receive:",
                        "\tset {channels::*} to voice channels of event-guild",
                        "\tloop {channels::*}:",
                        "\t\tbroadcast \"%name of loop-value%\""
                );
    }

    private Expression<Object> object;

    @Override
    protected VoiceChannel[] get(Event e) {
        Object object = this.object.getSingle(e);
        if (object == null) {
            return null;
        }
        if (object instanceof Guild) {
            List<VoiceChannel> channels = ((Guild) object).getVoiceChannels();
            return ((Guild) object).getVoiceChannels().toArray(new VoiceChannel[channels.size()]);
        } else if (object instanceof Category) {
            List<VoiceChannel> channels = ((Category) object).getVoiceChannels();
            return ((Guild) object).getVoiceChannels().toArray(new VoiceChannel[channels.size()]);
        }
        return null;
    }

    @Override
    public boolean isSingle() {
        return false;
    }

    @Override
    public Class<? extends VoiceChannel> getReturnType() {
        return VoiceChannel.class;
    }

    @Override
    public String toString(Event e, boolean debug) {
        return "voice channels of " + object.toString(e, debug);
    }

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        object = (Expression<Object>) exprs[0];
        return true;
    }
}
