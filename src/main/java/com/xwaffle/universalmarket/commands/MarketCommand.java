package com.xwaffle.universalmarket.commands;

import com.xwaffle.universalmarket.UniversalMarket;
import com.xwaffle.universalmarket.market.MarketItem;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.EventContext;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.service.economy.Currency;
import org.spongepowered.api.service.economy.account.UniqueAccount;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

import java.math.BigDecimal;

/**
 * Created by Chase(Xwaffle) on 12/18/2017.
 */
public class MarketCommand extends BasicCommand {
    public MarketCommand() {
        super("", "The Main market Command.", "/market");
    }

    @Override
    public CommandResult process(CommandSource source, String arguments) throws CommandException {
        String[] args = arguments.split(" ");

        Player player = (Player) source;
        long expireTime = UniversalMarket.getInstance().getMarket().getExpireTime();
        long totalListings = UniversalMarket.getInstance().getMarket().getTotalItemsCanSell();


        if (arguments.isEmpty() || arguments.equalsIgnoreCase("")) {
            if (player.hasPermission("com.xwaffle.universalmarket.open")) {
                UniversalMarket.getInstance().getMarket().openMarket(player);
            } else {
                player.sendMessage(Text.of(TextColors.RED, "你没有权限打开市场。"));
            }
            return CommandResult.success();
        }

        if (args.length > 0) {
            switch (args[0].toLowerCase()) {
                case "open":
                case "o":
                    if (player.hasPermission("com.xwaffle.universalmarket.open")) {
                        UniversalMarket.getInstance().getMarket().openMarket(player);
                    } else {
                        player.sendMessage(Text.of(TextColors.RED, "你没有权限打开市场"));
                    }
                    break;
                case "add":
                case "a":


                    if (!player.hasPermission("com.xwaffle.universalmarket.add")) {
                        player.sendMessage(Text.of(TextColors.RED, "你没有权限打开市场"));
                        return CommandResult.success();
                    }

                    int listingCount = UniversalMarket.getInstance().getMarket().countListings(player.getUniqueId());
                    if (args.length < 2) {
                        player.sendMessage(Text.of(TextColors.RED, "非法命令！"));
                        player.sendMessage(Text.of(TextColors.YELLOW, "/um " + args[0].toLowerCase() + " (手持物品价格) (<可选参数> 数量)"));
                        return CommandResult.success();
                    }

                    if (listingCount >= totalListings) {
                        player.sendMessage(Text.of(TextColors.RED, "你已经一次出售了最大数量的物品。"));
                        return CommandResult.success();
                    }


                    if (UniversalMarket.getInstance().getMarket().isUsePermissionToSell()) {
                        System.out.println("User Perm Sell");
                        int userMaxSellPerm = 0;
                        for (int i = 1; i < 99; i++) {
                            if (player.hasPermission("com.xwaffle.universalmarket.addmax." + i)) {
                                userMaxSellPerm = i;
                            }
                        }


                        if (userMaxSellPerm <= listingCount) {
                            player.sendMessage(Text.of(TextColors.RED, "你已经达到了你在市场出售物品数量的最大上限。"));
                            player.sendMessage(Text.of(TextColors.RED, "你只有在市场出售", TextColors.GRAY, userMaxSellPerm, TextColors.RED, "个物品的权限"));
                            return CommandResult.success();
                        }

                    }


                    if (player.getItemInHand(HandTypes.MAIN_HAND).isPresent()) {
                        ItemStack stack = player.getItemInHand(HandTypes.MAIN_HAND).get();
                        double price;
                        try {
                            price = Double.parseDouble(args[1]);
                        } catch (Exception exc) {
                            player.sendMessage(Text.of(TextColors.RED, "物品价格参数不正确"));
                            player.sendMessage(Text.of(TextColors.YELLOW, "/um " + args[0].toLowerCase() + " (手持物品价格) (<可选参数> 数量)"));
                            return CommandResult.success();
                        }

                        int amount = stack.getQuantity();

                        if (args.length >= 3) {
                            try {
                                amount = Integer.parseInt(args[2]);
                                if (amount <= 0) {
                                    player.sendMessage(Text.of(TextColors.RED, "你必须输入一个正数价格，才能在市场上销售！"));
                                    return CommandResult.success();
                                }
                            } catch (Exception exc) {
                                player.sendMessage(Text.of(TextColors.RED, "物品数量参数不正确"));
                                player.sendMessage(Text.of(TextColors.YELLOW, "/um " + args[0].toLowerCase() + " (手持物品价格) (<可选参数> 数量)"));
                                return CommandResult.success();
                            }
                        }

                        if (UniversalMarket.getInstance().getMarket().useTax()) {
                            double tax = price * UniversalMarket.getInstance().getMarket().getTax();
                            UniqueAccount account = UniversalMarket.getInstance().getEconomyService().getOrCreateAccount(player.getUniqueId()).get();
                            Currency currency = UniversalMarket.getInstance().getEconomyService().getDefaultCurrency();
                            if (account.getBalance(currency).doubleValue() < tax) {
                                player.sendMessage(Text.of(TextColors.RED, "你付不起税！"));
                                player.sendMessage(Text.of(TextColors.RED, "你需要为这个物品支付", TextColors.YELLOW, UniversalMarket.getInstance().getMarket().getTax(), TextColors.RED, "。"));
                                player.sendMessage(Text.of(TextColors.RED, "你需要支付", TextColors.GREEN, tax, TextColors.RED, "才能够在市场上出售物品。"));
                                return CommandResult.success();
                            } else {
                                account.withdraw(currency, new BigDecimal(tax), Cause.of(EventContext.empty(), UniversalMarket.getInstance()));
                                player.sendMessage(Text.of(TextColors.RED, "商品售出税已经从你的账户上抽取了！"));
                                player.sendMessage(Text.of(TextColors.DARK_RED, "- $", TextColors.RED, tax));
                            }
                        }

                        if (UniversalMarket.getInstance().getMarket().payFlatPrice()) {
                            double flatPrice = UniversalMarket.getInstance().getMarket().getFlatPrice();
                            UniqueAccount account = UniversalMarket.getInstance().getEconomyService().getOrCreateAccount(player.getUniqueId()).get();
                            Currency currency = UniversalMarket.getInstance().getEconomyService().getDefaultCurrency();
                            if (account.getBalance(currency).doubleValue() < flatPrice) {
                                player.sendMessage(Text.of(TextColors.RED, "你需要支付", TextColors.GRAY, "$" + flatPrice, TextColors.RED, "才能够在市场上出售物品。"));
                                return CommandResult.success();
                            } else {
                                account.withdraw(currency, new BigDecimal(flatPrice), Cause.of(EventContext.empty(), UniversalMarket.getInstance()));
                                player.sendMessage(Text.of(TextColors.RED, "已经收了一笔市场费！"));
                                player.sendMessage(Text.of(TextColors.DARK_RED, "- $", TextColors.RED, flatPrice));
                            }
                        }

                        if (UniversalMarket.getInstance().getMarket().isItemBlacklisted(stack.getType())) {
                            player.sendMessage(Text.of(TextColors.RED, "这个物品无法被出售（" + stack.getType().getId() + "）"));
                            return CommandResult.success();
                        }


                        int prevAmount = stack.getQuantity();

                        if (amount == stack.getQuantity()) {
                            player.setItemInHand(HandTypes.MAIN_HAND, null);
                        } else {

                            if (amount > stack.getQuantity()) {
                                player.sendMessage(Text.of(TextColors.RED, "你无法出售超过你手持数量的物品。"));
                                return CommandResult.success();
                            }

                            stack.setQuantity(amount);
                        }


                        int id = UniversalMarket.getInstance().getDatabase().createEntry(stack.copy(), player.getUniqueId(), player.getName(), price, System.currentTimeMillis() + expireTime);
                        UniversalMarket.getInstance().getMarket().addItem(new MarketItem(id, stack.copy(), player.getUniqueId(), player.getName(), price, (System.currentTimeMillis() + expireTime)), false);
                        player.sendMessage(Text.of(TextColors.YELLOW, "物品已经添加到了", TextColors.GRAY, "UniversalMarket", TextColors.YELLOW, "，价格为 $", TextColors.DARK_AQUA, price));

                        if (amount != prevAmount) {
                            stack.setQuantity(prevAmount - amount);
                            player.setItemInHand(HandTypes.MAIN_HAND, stack);
                        }


                    } else {
                        player.sendMessage(Text.of(TextColors.RED, "手持物品来进行出售！"));
                    }


                    break;
                case "help":
                case "h":
                case "?":
                    player.sendMessage(Text.of(TextColors.DARK_AQUA, "Universal Market 帮助"));
                    player.sendMessage(Text.of(TextColors.YELLOW, "/um 或 /universalmarket"));
                    player.sendMessage(Text.of(TextColors.YELLOW, "/um a (手持物品价格) (<可选参数> 数量) 或 /um add  (手持物品价格) (<可选参数> 数量)", TextColors.GRAY, " - ", TextColors.GREEN, "将当前手持物品设定价格出售。"));
                    player.sendMessage(Text.of(TextColors.YELLOW, "/um o 或 /um open", TextColors.GRAY, " - ", TextColors.GREEN, "打开 Universal Market。"));
                    break;
            }
        } else {
            UniversalMarket.getInstance().getMarket().openMarket(player);
        }

        return CommandResult.success();
    }
}
