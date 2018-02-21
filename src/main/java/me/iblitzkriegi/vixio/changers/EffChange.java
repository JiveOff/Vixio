package me.iblitzkriegi.vixio.changers;

/**
 * This file is part of Skript.
 * <p>
 * Skript is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * Skript is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with Skript.  If not, see <http://www.gnu.org/licenses/>.
 * <p>
 * <p>
 * Copyright 2011-2017 Peter Güttinger and contributors
 */

import ch.njol.skript.Skript;
import ch.njol.skript.SkriptConfig;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.classes.Changer.ChangeMode;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.Variable;
import ch.njol.skript.log.CountingLogHandler;
import ch.njol.skript.log.ErrorQuality;
import ch.njol.skript.log.HandlerList;
import ch.njol.skript.log.LogHandler;
import ch.njol.skript.log.ParseLogHandler;
import ch.njol.skript.log.SkriptLogger;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.util.Patterns;
import ch.njol.skript.util.Utils;
import ch.njol.util.Kleenean;
import me.iblitzkriegi.vixio.Vixio;
import me.iblitzkriegi.vixio.util.ReflectionUtils;
import me.iblitzkriegi.vixio.util.Util;
import me.iblitzkriegi.vixio.util.wrapper.Bot;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Locale;
import java.util.logging.Level;

/**
 * @author Peter Güttinger
 * edited minorly for Vixio
 */
public class EffChange extends Effect {

    private static boolean parsing;

    private static Patterns<ChangeMode> patterns = new Patterns<>(new Object[][]{
            {"(add|give) %objects% to (%~objects%) with %bot/string%", ChangeMode.ADD},
            {"increase %~objects% by (%objects%) with %bot/string%", ChangeMode.ADD},
            {"give %~objects% (%objects%) as %bot/string%", ChangeMode.ADD},

            {"set %~objects% to (%objects%) with %bot/string%", ChangeMode.SET},

            {"remove (all|every) %objects% from (%~objects%) with %bot/string%", ChangeMode.REMOVE_ALL},

            {"(remove|subtract) %objects% from (%~objects%) with %bot/string%", ChangeMode.REMOVE},
            {"reduce %~objects% by (%objects%) with %bot/string%", ChangeMode.REMOVE},

            {"(delete|clear) (%~objects%) with %bot/string%", ChangeMode.DELETE},

            {"reset (%~objects%) with %bot/string%", ChangeMode.RESET}
    });

    static {
        Vixio.getInstance().registerEffect(EffChange.class, patterns.getPatterns());
    }

    @SuppressWarnings("null")
    private Expression<?> changed;
    @Nullable
    private Expression<?> changer = null;

    private Expression<Bot> bot;

    @SuppressWarnings("null")
    private ChangeMode mode;

    private boolean single;

//	private Changer<?, ?> c = null;

    @SuppressWarnings({"unchecked", "null"})
    @Override
    public boolean init(final Expression<?>[] exprs, final int matchedPattern, final Kleenean isDelayed, final ParseResult parser) {
        parsing = true;
        mode = patterns.getInfo(matchedPattern);
        bot = (Expression<Bot>) exprs[2];

        switch (mode) {
            case ADD:
                if (matchedPattern == 0) {
                    changer = exprs[0];
                    changed = exprs[1];
                } else {
                    changer = exprs[1];
                    changed = exprs[0];
                }
                break;
            case SET:
                changer = exprs[1];
                changed = exprs[0];
                break;
            case REMOVE_ALL:
                changer = exprs[0];
                changed = exprs[1];
                break;
            case REMOVE:
                if (matchedPattern == 5) {
                    changer = exprs[0];
                    changed = exprs[1];
                } else {
                    changer = exprs[1];
                    changed = exprs[0];
                }
                break;
            case DELETE:
                changed = exprs[0];
                break;
            case RESET:
                changed = exprs[0];
        }
        if (!(changed instanceof ChangeablePropertyExpression || changed instanceof ChangeableSimpleExpression || changed instanceof ChangeableSimplePropertyExpression)) {
            Skript.error(changed.toString(null, false) + " can't be changed with Vixio's changer effects");
            parsing = false;
            return false;
        }

        final CountingLogHandler h = SkriptLogger.startLogHandler(new CountingLogHandler(Level.SEVERE));
        final Class<?>[] rs;
        final String what;
        try {
            rs = changed.acceptChange(mode);
            final ClassInfo<?> c = Classes.getSuperClassInfo(changed.getReturnType());
            final Changer<?> changer = c.getChanger();
            what = changer == null || !Arrays.equals(changer.acceptChange(mode), rs) ? changed.toString(null, false) : c.getName().withIndefiniteArticle();
        } finally {
            h.stop();
        }
        if (rs == null) {
            if (h.getCount() > 0) {
                parsing = false;
                return false;
            }
            switch (mode) {
                case SET:
                    Skript.error(what + " can't be set to anything", ErrorQuality.SEMANTIC_ERROR);
                    break;
                case DELETE:
                    if (changed.acceptChange(ChangeMode.RESET) != null)
                        Skript.error(what + " can't be deleted/cleared. It can however be reset which might result in the desired effect.", ErrorQuality.SEMANTIC_ERROR);
                    else
                        Skript.error(what + " can't be deleted/cleared", ErrorQuality.SEMANTIC_ERROR);
                    break;
                case REMOVE_ALL:
                    if (changed.acceptChange(ChangeMode.REMOVE) != null) {
                        Skript.error(what + " can't have 'all of something' removed from it. Use 'remove' instead of 'remove all' to fix this.", ErrorQuality.SEMANTIC_ERROR);
                        break;
                    }
                    //$FALL-THROUGH$
                case ADD:
                case REMOVE:
                    Skript.error(what + " can't have anything " + (mode == ChangeMode.ADD ? "added to" : "removed from") + " it", ErrorQuality.SEMANTIC_ERROR);
                    break;
                case RESET:
                    if (changed.acceptChange(ChangeMode.DELETE) != null)
                        Skript.error(what + " can't be reset. It can however be deleted which might result in the desired effect.", ErrorQuality.SEMANTIC_ERROR);
                    else
                        Skript.error(what + " can't be reset", ErrorQuality.SEMANTIC_ERROR);
            }
            parsing = false;
            return false;
        }

        final Class<?>[] rs2 = new Class<?>[rs.length];
        for (int i = 0; i < rs.length; i++)
            rs2[i] = rs[i].isArray() ? rs[i].getComponentType() : rs[i];
        final boolean allSingle = Arrays.equals(rs, rs2);

        Expression<?> ch = changer;
        if (ch != null) {
            Expression<?> v = null;
            final ParseLogHandler log = SkriptLogger.startParseLogHandler();
            try {
                for (final Class<?> r : rs) {
                    log.clear();
                    if ((r.isArray() ? r.getComponentType() : r).isAssignableFrom(ch.getReturnType())) {
                        v = ch.getConvertedExpression(Object.class);
                        break; // break even if v == null as it won't convert to Object apparently
                    }
                }
                if (v == null)
                    v = ch.getConvertedExpression((Class<Object>[]) rs2);
                if (v == null) {
                    if (log.hasError()) {
                        log.printError();
                        parsing = false;
                        return false;
                    }
                    log.clear();
                    log.printLog();
                    final Class<?>[] r = new Class[rs.length];
                    for (int i = 0; i < rs.length; i++)
                        r[i] = rs[i].isArray() ? rs[i].getComponentType() : rs[i];
                    if (rs.length == 1 && rs[0] == Object.class)
                        Skript.error("Can't understand this expression: " + changer, ErrorQuality.NOT_AN_EXPRESSION);
                    else if (mode == ChangeMode.SET)
                        Skript.error(what + " can't be set to " + changer + " because the latter is " + SkriptParser.notOfType(r), ErrorQuality.SEMANTIC_ERROR);
                    else
                        Skript.error(changer + " can't be " + (mode == ChangeMode.ADD ? "added to" : "removed from") + " " + what + " because the former is " + SkriptParser.notOfType(r), ErrorQuality.SEMANTIC_ERROR);
                    parsing = false;
                    return false;
                }
                log.printLog();
            } finally {
                log.stop();
            }

            Class<?> x = Utils.getSuperType(rs2);
            single = allSingle;
            for (int i = 0; i < rs.length; i++) {
                if (rs2[i].isAssignableFrom(v.getReturnType())) {
                    single = !rs[i].isArray();
                    x = rs2[i];
                    break;
                }
            }
            assert x != null;
            changer = ch = v;

            if (!ch.isSingle() && single) {
                if (mode == ChangeMode.SET)
                    Skript.error(changed + " can only be set to one " + Classes.getSuperClassInfo(x).getName() + ", not more", ErrorQuality.SEMANTIC_ERROR);
                else
                    Skript.error("only one " + Classes.getSuperClassInfo(x).getName() + " can be " + (mode == ChangeMode.ADD ? "added to" : "removed from") + " " + changed + ", not more", ErrorQuality.SEMANTIC_ERROR);
                parsing = false;
                return false;
            }

            if (changed instanceof Variable && !((Variable<?>) changed).isLocal() && (mode == ChangeMode.SET || ((Variable<?>) changed).isList() && mode == ChangeMode.ADD)) {
                final ClassInfo<?> ci = Classes.getSuperClassInfo(ch.getReturnType());
                if (ci.getC() != Object.class && ci.getSerializer() == null && ci.getSerializeAs() == null && !SkriptConfig.disableObjectCannotBeSavedWarnings.value())
                    Skript.warning(ci.getName().withIndefiniteArticle() + " cannot be saved, i.e. the contents of the variable " + changed + " will be lost when the server stops.");
            }
        }
        parsing = false;
        return true;
    }

    @Override
    protected void execute(final Event e) {
        final Expression<?> changer = this.changer;
        final Object[] delta = changer == null ? null : changer.getArray(e);
        final Bot bot = Util.botFrom(this.bot.getSingle(e));
        if (delta == null || delta.length == 0) {
            return;
        }
        if (bot == null) {
            Vixio.getErrorHandler().warn("Vixio tried to change \"" + changed.toString(e, false) + "\", but the bot wasn't found.");
            return;
        }

        if (changed instanceof ChangeableSimplePropertyExpression) {
            ((ChangeableSimplePropertyExpression) changed).change(e, delta, bot, mode);
        } else if (changed instanceof ChangeableSimpleExpression) {
            ((ChangeableSimpleExpression) changed).change(e, delta, bot, mode);
        } else if (changed instanceof ChangeablePropertyExpression) {
            ((ChangeablePropertyExpression) changed).change(e, delta, bot, mode);
        } else if (changed instanceof ChangeableExpression) {
            ((ChangeableExpression) changed).change(e, delta, bot, mode);
        }

//		changed.change(e, new Changer2<Object>() {
//			@Override
//			public Object change(Object o) {
//				return delta;
//			}
//		}, mode);
    }

    @Override
    public String toString(final @Nullable Event e, final boolean debug) {
        final Expression<?> changer = this.changer;
        switch (mode) {
            case ADD:
                assert changer != null;
                return "add " + changer.toString(e, debug) + " to " + changed.toString(e, debug) + " with " + bot.toString(e, debug);
            case SET:
                assert changer != null;
                return "set " + changed.toString(e, debug) + " to " + changer.toString(e, debug) + " with " + bot.toString(e, debug);
            case REMOVE:
                assert changer != null;
                return "remove " + changer.toString(e, debug) + " from " + changed.toString(e, debug) + " with " + bot.toString(e, debug);
            case REMOVE_ALL:
                assert changer != null;
                return "remove all " + changer.toString(e, debug) + " from " + changed.toString(e, debug) + " with " + bot.toString(e, debug);
            case DELETE:
                return "delete/clear " + changed.toString(e, debug) + " with " + bot.toString(e, debug);
            case RESET:
                return "reset " + changed.toString(e, debug) + " with " + bot.toString(e, debug);
        }
        assert false;
        return "";
    }

    public static boolean isParsing(Expression<?> expression) {
        if (parsing) {
            return true;
        }
        Skript.error(expression.toString(null, false) + " can only be changed using Vixio's changer effects");
        // Handler editing to avoid Skript's changer errors
        HandlerList handlers = ReflectionUtils.getField(SkriptLogger.class, null, "handlers");
        if (handlers == null) {
            return false;
        }
        for (Iterator<LogHandler> iterator = handlers.iterator(); iterator.hasNext();) {
            LogHandler handler = iterator.next();
            if (handler instanceof CountingLogHandler) {
                ReflectionUtils.setField(CountingLogHandler.class, handler, "count", -1);
            }
        }
        return false;
    }

    public static String format(ChangeMode mode, String prop, Expression<?> changed, Bot bot) {
        return mode.name().toLowerCase(Locale.ENGLISH) + " " + prop + " " + changed.toString(null, false) + " with \"" + bot.toString() + "\"";
    }

}
