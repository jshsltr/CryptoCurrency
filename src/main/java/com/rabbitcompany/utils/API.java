package com.rabbitcompany.utils;

import com.rabbitcompany.CryptoCurrency;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

public class API {

    public static NumberFormat moneyFormatter = new DecimalFormat("#" + CryptoCurrency.getInstance().getConf().getString("money_format", "###,###.00"));

    public static boolean isCryptoEnabled(String crypto){
        return Settings.cryptos.get(crypto) != null;
    }

    public static List<String> getEnabledCryptos(){
        return new ArrayList<>(Settings.cryptos.keySet());
    }

    public static String getAPICurrency(){
        return CryptoCurrency.getInstance().getConf().getString("api_currency", "USD");
    }

    public static NumberFormat getFormatter(String crypto){
        if(!isCryptoEnabled(crypto)) return new DecimalFormat("#0.0000");
        return new DecimalFormat("#" + Settings.cryptos.get(crypto).format);
    }

    public static boolean reloadCrypto(String crypto){
        if(!isCryptoEnabled(crypto)) return false;
        Settings.cryptos.get(crypto).initializeWallet();
        return true;
    }

    public static void reloadConfigFiles(){
        CryptoCurrency.getInstance().loadYamls();
    }

    public static String getUUID(String player){
        return CryptoCurrency.getInstance().getPlayers().getString(player, null);
    }

    public static boolean hasWallet(String player){
        return CryptoCurrency.getInstance().getPlayers().getString(player, null) != null;
    }

    public static double getBalance(String player, String crypto){
        if(!isCryptoEnabled(crypto)) return 0;
        if(!hasWallet(player)) return 0;
        String UUID = getUUID(player);
        if(CryptoCurrency.conn != null) return MySql.getPlayerBalance(UUID, player, crypto);
        return Settings.cryptos.get(crypto).wallet.getDouble(UUID, 0);
    }

    public static String getBalanceFormatted(String player, String crypto){
        return getFormatter(crypto).format(getBalance(crypto, player));
    }

    public static double getCryptoPrice(String crypto){
        if(!isCryptoEnabled(crypto)) return 0;
        return Settings.cryptos.get(crypto).price;
    }

    public static double getCryptoPrice(String crypto, double amount){
        return getCryptoPrice(crypto) * amount;
    }

    public static String getCryptoPriceFormatted(String crypto){
        return moneyFormatter.format(getCryptoPrice(crypto));
    }

    public static String getCryptoPriceFormatted(String crypto, double amount){
        return moneyFormatter.format(getCryptoPrice(crypto, amount));
    }

    public static int giveCrypto(String toPlayer, String crypto, double amount){
        if(!isCryptoEnabled(crypto)) return 1;
        if(amount < Settings.cryptos.get(crypto).minimum) return 2;
        if(amount > Settings.cryptos.get(crypto).maximum) return 3;
        if(!hasWallet(toPlayer)) return 4;
        String UUID = getUUID(toPlayer);
        double balance = getBalance(toPlayer, crypto);
        if(CryptoCurrency.conn != null){
            MySql.setPlayerBalance(UUID, toPlayer, getFormatter(crypto).format(balance + amount), crypto);
            return 10;
        }
        Settings.cryptos.get(crypto).wallet.set(UUID, balance + amount);
        Settings.cryptos.get(crypto).saveWallet();
        return 10;
    }

    public static int buyCrypto(String player, String crypto, double amount){
        if(!CryptoCurrency.vault) return 9;
        if(!isCryptoEnabled(crypto)) return 1;
        if(amount < Settings.cryptos.get(crypto).minimum) return 2;
        if(amount > Settings.cryptos.get(crypto).maximum) return 3;
        if(!hasWallet(player)) return 4;
        String UUID = getUUID(player);
        double balance = getBalance(player, crypto);
        double player_balance = CryptoCurrency.getEconomy().getBalance(player);
        double money_price = getCryptoPrice(crypto, amount);
        if(player_balance < money_price) return 11;
        if(CryptoCurrency.conn != null){
            MySql.setPlayerBalance(UUID, player, API.getFormatter(crypto).format(balance + amount), crypto);
            CryptoCurrency.getEconomy().withdrawPlayer(player, money_price);
            return 10;
        }
        Settings.cryptos.get(crypto).wallet.set(UUID, balance + amount);
        Settings.cryptos.get(crypto).saveWallet();
        CryptoCurrency.getEconomy().withdrawPlayer(player, money_price);
        return 10;
    }

    public static int takeCrypto(String fromPlayer, String crypto, double amount){
        if(!isCryptoEnabled(crypto)) return 1;
        if(amount < Settings.cryptos.get(crypto).minimum) return 2;
        if(amount > Settings.cryptos.get(crypto).maximum) return 3;
        if(!hasWallet(fromPlayer)) return 4;
        String UUID = getUUID(fromPlayer);
        double balance = getBalance(fromPlayer, crypto);
        if(balance - amount < 0) amount = balance;
        if(CryptoCurrency.conn != null){
            MySql.setPlayerBalance(UUID, fromPlayer, getFormatter(crypto).format(balance - amount), crypto);
            return 10;
        }
        Settings.cryptos.get(crypto).wallet.set(UUID, balance - amount);
        Settings.cryptos.get(crypto).saveWallet();
        return 10;
    }

    public static int sellCrypto(String player, String crypto, double amount){
        if(!CryptoCurrency.vault) return 9;
        if(!isCryptoEnabled(crypto)) return 1;
        if(amount < Settings.cryptos.get(crypto).minimum) return 2;
        if(amount > Settings.cryptos.get(crypto).maximum) return 3;
        if(!hasWallet(player)) return 4;
        String UUID = getUUID(player);
        double balance = getBalance(player, crypto);
        double money_price = getCryptoPrice(crypto, amount);
        if(balance < amount) return 11;
        if(CryptoCurrency.conn != null){
            MySql.setPlayerBalance(UUID, player, API.getFormatter(crypto).format(balance - amount), crypto);
            CryptoCurrency.getEconomy().depositPlayer(player, money_price);
            return 10;
        }
        Settings.cryptos.get(crypto).wallet.set(UUID, balance - amount);
        Settings.cryptos.get(crypto).saveWallet();
        CryptoCurrency.getEconomy().depositPlayer(player, money_price);
        return 10;
    }

    public static int sendCrypto(String fromPlayer, String toPlayer, String crypto, double amount){
        if(!isCryptoEnabled(crypto)) return 1;
        if(amount < Settings.cryptos.get(crypto).minimum) return 2;
        if(amount > Settings.cryptos.get(crypto).maximum) return 3;
        if(!hasWallet(fromPlayer)) return 4;
        if(!hasWallet(toPlayer)) return 5;
        double fromBalance = getBalance(crypto, fromPlayer);

        if(fromBalance < amount) return 6;
        if(takeCrypto(fromPlayer, crypto, amount) != 10) return 7;
        if(giveCrypto(toPlayer, crypto, amount) != 10) return 8;
        return 10;
    }
}
