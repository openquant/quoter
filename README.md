Quoter
======

Manage database of historical quotes for backtesting


Dependencies
------------

The scala build tool [SBT](http://www.scala-sbt.org/download.html) and a working JDK are the only requirements to run the project.

For development, intellij with the scala plugin is recommended. Just import the project as an "SBT project"


Database
--------

The database has two tables, "quotes" and "subscriptions".

Quotes for all contracts are stored in the "quotes" table, including quotes for different time intervals.

A subscription denotes interest on keeping a contract up to date, so running a generic update process without a specific contract will update all the contracts which have a subscription.

Usage examples
--------------

	sbt assembly
	alias quoter="java -jar `pwd`/target/scala-2.11/trabot-assembly-0.1.jar"

`update` will make sure we have the latest data for a contract

	quoter update -s IBM

A subscription can be added with

	quoter subscribe -s MSFT

Running update will update all the contracts in subscriptions

	quoter update

You can see the data by running:

	sqlite3 quotes.db "select * from quotes"

