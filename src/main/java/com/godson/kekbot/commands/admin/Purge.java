package com.godson.kekbot.commands.admin;

import com.darichey.discord.api.Command;
import com.darichey.discord.api.CommandCategory;
import com.darichey.discord.api.FailureReason;
import com.godson.kekbot.KekBot;
import com.godson.kekbot.Responses.Action;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Game;
import net.dv8tion.jda.core.entities.Icon;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import org.apache.commons.lang3.StringUtils;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


public class Purge {
    public static Command purge = new Command("purge")
            .withAliases("prune", "clean")
            .withCategory(CommandCategory.ADMIN)
            .withDescription("Mass deletes X number of messages. Supply a keyphrase or a mention {or multiple mentions} to purge all messages within X number of messages.")
            .withUsage("{p}purge <number> {keyphrase or @mention(s)}")
            .userRequiredPermissions(Permission.MESSAGE_MANAGE)
            .botRequiredPermissions(Permission.MESSAGE_MANAGE)
            .onExecuted(context -> {
                String args[] = context.getArgs();
                TextChannel channel = context.getTextChannel();
                if (args.length == 0) {
                    channel.sendMessage(context.getAuthor().getAsMention() + ", next time, try to at least supply a number...").queue();
                } else if (args.length >= 1) {
                    try {
                        int purge = Integer.valueOf(args[0]);
                        if (purge <= 1) {
                            channel.sendMessage(KekBot.respond(context, Action.PURGE_TOOLOW)).queue();
                        } else if (purge > 100) {
                            channel.sendMessage(KekBot.respond(context, Action.PURGE_TOOHIGH)).queue();
                        } else if (purge >= 2 && purge <= 100) {
                            context.getMessage().delete().queue();
                            channel.getHistory().retrievePast(purge).queue(msgsRaw -> {
                                List<Message> msgs = msgsRaw.stream().filter(message -> !message.getCreationTime().plusWeeks(2).isBefore(OffsetDateTime.now())).collect(Collectors.toList());
                                channel.sendMessage("Purging...").queue(msg -> {
                                    if (msgs.size() > 0) {
                                        if (args.length >= 2) {
                                            StringBuilder builder = new StringBuilder();
                                            for (int i = 1; i < args.length; i++) {
                                                if (!(args[i].equals("") && builder.length() < 1))
                                                    builder.append(args[i]);
                                                if (i + 1 != args.length) builder.append(" ");
                                            }
                                            String keyphrase = builder.toString();
                                            if (context.getMessage().getMentionedUsers().size() == 0) {
                                                List<Message> keywordPurge = msgs.stream().filter(mes -> mes.getRawContent().toLowerCase().contains(keyphrase.toLowerCase())).collect(Collectors.toList());
                                                if (keywordPurge.size() > 1)
                                                    channel.deleteMessages(keywordPurge).queue();
                                                else if (keywordPurge.size() == 1)
                                                    keywordPurge.get(0).delete().queue();
                                                if (keywordPurge.size() >= 1)
                                                    msg.editMessage(KekBot.respond(context, Action.KEYPHRASE_PURGE_SUCCESS, "`" + keywordPurge.size() + "`", "`" + keyphrase + "`")).queue();
                                                else
                                                    msg.editMessage(KekBot.respond(context, Action.KEYPHRASE_PURGE_FAIL)).queue();
                                            } else {
                                                List<String> mentions = new ArrayList<String>();
                                                context.getMessage().getMentionedUsers().forEach(user -> mentions.add(user.getAsMention()));
                                                Pattern p = Pattern.compile("([A-Z])+", Pattern.CASE_INSENSITIVE);
                                                Matcher m = p.matcher(keyphrase);
                                                if (mentions.stream().anyMatch(mentions::contains) && m.find()) {
                                                    msg.editMessage("Sorry, but you can't type key-phrases and mentions while attempting to purge.").queue();
                                                } else {
                                                    List<Message> mentionPurge = msgs.stream().filter(mes -> context.getMessage().getMentionedUsers().stream().anyMatch(mes.getAuthor()::equals)).collect(Collectors.toList());
                                                    if (mentionPurge.size() > 1)
                                                        channel.deleteMessages(mentionPurge).queue();
                                                    else if (mentionPurge.size() == 1)
                                                        mentionPurge.get(0).delete().queue();
                                                    if (mentionPurge.size() >= 1)
                                                        msg.editMessage(KekBot.respond(context, Action.MENTION_PURGE_SUCCESS, "`" + mentionPurge.size() + "`", "`" + StringUtils.join(context.getMessage().getMentionedUsers().stream().map(user -> user.getName() + "#" + user.getDiscriminator()).collect(Collectors.toList()), ", ") + "`")).queue();
                                                    else
                                                        msg.editMessage(KekBot.respond(context, Action.MENTION_PURGE_FAIL)).queue();
                                                }
                                            }
                                        } else {
                                            if (msgs.size() == 1)
                                                msgs.get(0).delete().queue(delet -> msg.editMessage(KekBot.respond(context, Action.PURGE_SUCCESS, "`" + msgs.size() + "`")).queue());
                                            else
                                                channel.deleteMessages(msgs).queue(delet -> msg.editMessage(KekBot.respond(context, Action.PURGE_SUCCESS, "`" + msgs.size() + "`")).queue());
                                        }
                                    } else msg.editMessage("Either all the messages I've found are 2+ weeks old, or there are no messages to purge at all.").queue();
                                });
                            });
                        }
                    } catch (NumberFormatException e) {
                        channel.sendMessage(KekBot.respond(context, Action.NOT_A_NUMBER, "`" + args[0] + "`")).queue();
                    }
                }
            })
            .onFailure((context, failureReason) -> {
                if (failureReason.equals(FailureReason.AUTHOR_MISSING_PERMISSIONS))
                    context.getTextChannel().sendMessage(KekBot.respond(context, Action.NOPERM_USER, "`Manage Messages`")).queue();
                else context.getTextChannel().sendMessage(KekBot.respond(context, Action.NOPERM_BOT, "`Manage Messages`")).queue();
            });
}
