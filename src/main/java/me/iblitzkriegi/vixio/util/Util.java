package me.iblitzkriegi.vixio.util;

import ch.njol.skript.Skript;
import me.iblitzkriegi.vixio.Vixio;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.Message;

import java.awt.Color;
import java.util.Locale;

public class Util {

    public static Color getColorFromString(String str) {

        if (str == null) return null;

        Color color = null;

        try {
            color = (Color) Color.class.getField(str.toUpperCase(Locale.ENGLISH).replace(" ", "_")).get(null);
        } catch (NoSuchFieldException e) {
        } catch (IllegalAccessException e1) {
            Skript.exception(e1);
        }

        return color;

    }

    public static Bot botFrom(Object input){
        if(input instanceof Bot){
            return (Bot) input;
        }else if(input instanceof String){
            String string = (String) input;
            Bot bot = Vixio.getInstance().botNameHashMap.get(string);
            if(bot != null){
                return bot;
            }
            Skript.error("Could not find bot by that name!");
        }
        Skript.error("Could not find bot by that name!");
        return null;
    }

    public static Message messageFrom(Object input){
        if(input instanceof Message) {
            return (Message) input;
        }else if(input instanceof String){
            return new MessageBuilder()
                    .setContent((String) input)
                    .build();
        }
        Skript.error("You must input either a %message/messagebuilder/embedbuilder% or a %string%");
        return null;
    }

}