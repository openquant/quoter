#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Description"""

__author__ = 'Pedro Larroy'
__version__ = '0.1'

import os
import sys
import subprocess
import urllib.request
import datetime
from bs4 import BeautifulSoup

def getSoup(url):
    req = urllib.request.Request(url, data = None, headers = {
        'User-Agent': 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_9_3) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/35.0.1916.47 Safari/537.36'
    })
    f = urllib.request.urlopen(req)
    content = f.read().decode('utf-8')
    soup = BeautifulSoup(content)
    return soup

def getFtable(soup):
    table = soup.find("table", attrs={"class": "cmeTable cmeCalendarFutures"})
    return table

def getSettlements(table):
    settlements = []
    rows = table.find_all("tr")[2:]
    for row in rows:
        #print(row)
        settlement = row.find_all("td")[2]
        settlements.append(settlement.string.strip())
    return settlements

def getContracts(table):
    xs = []
    rows = table.find_all("tr")[2:]
    for row in rows:
        x = row.find_all("td")[0]
        xs.append(x.string.strip())
    return xs



def main():
    url = 'http://www.cmegroup.com/trading/energy/crude-oil/light-sweet-crude_product_calendar_futures.html'
    table = getFtable(getSoup(url))
    settlements = getSettlements(table)
    contracts = getContracts(table)
    print("contract,month,settlement,settlementISO")
    for r in zip(contracts, settlements):
        dt = datetime.datetime.strptime(r[1], "%d %b %Y")
        row = [r[0], dt.strftime("%b"), r[1], dt.isoformat()]
        print(",".join(row))
    return 0

if __name__ == '__main__':
    sys.exit(main())

