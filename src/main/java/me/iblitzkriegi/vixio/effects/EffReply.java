package me.iblitzkriegi.vixio.effects;

import ch.njol.skript.ScriptLoader;
import ch.njol.skript.Skript;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import me.iblitzkriegi.vixio.events.EvntGuildMessageBotSend;
import me.iblitzkriegi.vixio.events.EvntGuildMessageReceive;
import me.iblitzkriegi.vixio.events.EvntPrivateMessageReceive;
import me.iblitzkriegi.vixio.registration.EffectAnnotation;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.Channel;
import net.dv8tion.jda.core.entities.PrivateChannel;
import net.dv8tion.jda.core.entities.TextChannel;
import org.bukkit.event.Event;

import javax.xml.soap.Text;

/**
 * Created by Blitz on 10/30/2016.
 */
@EffectAnnotation.Effect(
        name = "ReplytoaMessage",
        title = "Reply to a Message",
        desc = "Reply to a Message in a event",
        syntax = "[discord ]reply with %string%",
        example = "SOON"
)
public class EffReply extends Effect{
    private Expression<String> message;
    @Override
    protected void execute(Event e) {
        if(e instanceof EvntGuildMessageReceive) {
            JDA jda = ((EvntGuildMessageReceive)e).getJDA();
            TextChannel chnl = (TextChannel) ((EvntGuildMessageReceive) e).getEvntChannel();
            jda.getTextChannelById(chnl.getId()).sendMessage(message.getSingle(e)).queue();

        }else if(e instanceof EvntPrivateMessageReceive){
            JDA jda = ((EvntPrivateMessageReceive)e).getJDA();
            PrivateChannel id = ((EvntPrivateMessageReceive)e).getEvntChannel();
            jda.getPrivateChannelById(id.getId()).sendMessage(message.getSingle(e)).queue();
        }else if (e instanceof EvntGuildMessageBotSend){
            JDA jda = ((EvntGuildMessageBotSend)e).getJDA();
            TextChannel chnl = ((EvntGuildMessageBotSend)e).getEvntTextChannel();
            jda.getTextChannelById(chnl.getId()).sendMessage(message.getSingle(e)).queue();

        }else{
            Skript.warning("You dun diddly dun did fuk up famberino. Bouta sassarino u four eit rn. Damn son, next tiem u might want sum lube b4 u get fuked by dis error.");
        }
    }

    @Override
    public String toString(Event e, boolean b) {
        return getClass().getName();
    }

    @Override
    public boolean init(Expression<?>[] expr, int i, Kleenean kleenean, SkriptParser.ParseResult parseResult) {
        if(ScriptLoader.isCurrentEvent(EvntGuildMessageReceive.class) || ScriptLoader.isCurrentEvent(EvntPrivateMessageReceive.class) || ScriptLoader.isCurrentEvent(EvntGuildMessageBotSend.class)) {
            message = (Expression<String>) expr[0];
            return true;
        }
        Skript.warning("You may not use \"reply with\" out of Discord events!");
        return false;
    }
}
