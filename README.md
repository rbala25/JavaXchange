# JavaXchange

A multithreaded Java-based stock simulator of trading bots using different financial indicators and well-known technical analysis tools.

## Data
Includes a data.txt file in the src/com/rishibala package which has multiple pieces of information on the price of AAPL over the course of June 2021 to January 2023. All data in the file was received from the [Twelve Data Api](https://twelvedata.com/login). The source file WriteData in the src/com/rishibala package is only needed once to produce the data in the data.txt file, but my specific API key is not given. All file paths will also have to be changed if run again.

## Server
The src/com/rishibala/server package holds multiple source files, including the main file: StockExchange.java. For each bot that joins the ServerSocket, a new instance of the BotHandler class is created, which manages all IO to each bot and sends its orders to the OrderBook. This BotHandler class implements the Java interface Runnable, allowing each instance to be executed by a thread. The OrderBook is a class that holds two SortedMaps, one for buy orders and one for sell orders. Each map is sorted from highest price to lowest, and when orders are filled or canceled, the OrderBook is updated to remove them.

## Trading Bots
The src/com/rishibala/bots package holds 5 trading bots with distinct strategies, as well as a MarketMakingBot which is not actually designed to make a profit, but rather to put one buy and one sell order into the market every 50 milliseconds, based off each aforementioned data point from the AAPL stock. Each buy order has a price that is a little below the actual price of AAPL at that moment, and each sell order is a little above. Specifically, each buy order is 0.11554744394 below the price, and each sell order is the same amount above. This number is exactly 1/10th of the Coefficient of Variation of AAPL during the time period of June 2021 to January 2023. This Coefficient of Variation was determined in src/com/rishibala/CalculateMetrics.java. Altogether, the Market Maker is designed to simulate a somewhat realistic market. Every individual trading bot inherits from the abstract class Bot in the src/com/rishibala/bots package, which itself implements the Java interface Runnable, allowing each bot to be further executed by a thread.

#### Exponentially Weighted Moving Average (EWMA) Trading Bot
This specific trading bot calculates the Exponentially Weighted Moving Average, one of the most commonly used trading strategies and financial indicators, of AAPL at any given time. At most, it takes into account a period of the last 14,000 orders put on the market. The EWMA is one type of moving average that places a greater emphasis on more recent data points. Specifically, the bot uses an alpha of 0.2, which is a metric that signifies how important the current observation is in the calculation of the EWMA. When the EWMA falls below the buy price, the bot considers selling, and when it is greater than the sell price, the bot considers buying.

#### Simple Moving Average (SMA) Trading Bot
The Simple Moving Average is another type of moving average, just like the aforementioned EWMA, however, it is far simpler as it does not put more emphasis on any specific type of data point. Instead, this trading bot essentially calculates the arithmetic mean of the prices of any given period. Just like the EWMA, when the SMA falls below the buying price, the bot considers selling, and when it is greater than the selling price, the bot considers buying.

#### Bollinger Bands Trading Bot
Bollinger Bands is another very commonly used financial analysis tool. This trading bot uses three bands rather than the usual two, an UpperBand, a LowerBand, and a MiddleBand. The MiddleBand is just the Simple Moving Average, but the Upper and Lower bands are statistics plotted at a standard deviation level above and below the SMA, respectively. These bands demonstrate when the stock is 'overbought' or 'oversold' and provide insight into when to consider buying and when to consider selling.

#### RSI (Relative Strength Index) Trading Bot
The RSI is another technical indicator that measures the volatility and change in the stock. The final number is between 0 and 100 and can further demonstrate when a stock is 'overbought' or 'oversold'. Traditionally, the RSI is considered overbought at 70 or above and oversold at 30 or below. However, with relatively stable stocks, such as AAPL, it is far rarer for the RSI to ever drop to 30 or rise to 70, so this range was brought closer to 40 and 60 in order to determine when to buy or sell.

#### Fibonacci Retracement Trading Bot
This strategy is slightly less well-known, but it is another technical analysis tool that is found by taking the max and min of a given period and dividing the range between them by the key Fibonacci ratios: 23.6%, 38.2%, 50%, 61.8%, and 78.6%. The bot uses the 23.6% level and the 78.6% level to determine buy and sell signals. 

## Setup
Download the repository or clone it onto your local machine. If you want to rewrite the data.txt file, use the WriteData.java file, change the file path, and create your own [Twelve Data Api](https://twelvedata.com/login) key. Otherwise, change the file path to your own data.txt file in the parseConstantData() method of the src/com/rishibala/bots/MarketMaker file. There is a ManualTradingBot in the src/com/rishibala/oldVersions package, but this was only used for inital testing, so is likely not very useful or functional by this point. This "oldVersions" package also includes all of the aforementioned trading bots but these versions do not support multithreading or running multiple instances of the bot at once.

### Command Line Commands
The following commands to start the server and the trading bots were designed for MacOS, but some may also work for other Operating Systems.

#### Compile all files:
Navigate to the src folder with "cd" before both of these commands.
```
javac com/rishibala/server/*.java com/rishibala/bots/*.java com/rishibala/start.java
```

#### Start server and trading bots:
Use the following keywords or similar variations to start however many instances of whatever bot you want.

```
java com/rishibala/start Bollinger EWMA SMA Fib RSI
```

### Alternative Command Line Commands
#### Manually Start the Server and Trading Bots:
Make sure to navigate to the src folder with "cd" prior to every command and to start the MarketMaker as the first bot directly after starting the StockExchange file. The manual trading bot in the src/com/rishibala/oldVersions package was only used for initial testing and is likely not very useful or functional. Use the following three commands in order, ensuring that StockExchange is the first file started, followed by the MarketMaker.

```
javac com/rishibala/server/*.java com/rishibala/bots/*.java com/rishibala/start.java
```

```
java com.rishibala.server.StockExchange
```

```
java com.rishibala.bots.MarketMaker
```

```
java com.rishibala.bots.EWMABot
```

Replace "EWMABot" in the final command with any of:
  - SMABot
  - BollingerBandsBot
  - RSIBot
  - FibonacciRetracementBot

