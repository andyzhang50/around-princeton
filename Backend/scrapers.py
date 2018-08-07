from bs4 import BeautifulSoup
import requests

from os import path
import csv

from database import Database
from dataobjects import Facility, FacilityType, Building

'''
TODO: 
Includes Unicode Strings, 
figure out how to remove them (do I want to remove them? <- I removed them)
'''
'''
import sys  
reload(sys)  
sys.setdefaultencoding('utf8')
'''

def get_soup(url):
        try:
            page = requests.get(url)
        except:
            raise Exception('Get Soup Failed')
        if page.status_code == 404:
            raise Exception('Get Soup Failed')
        soup = BeautifulSoup(page.content, 'html.parser')
        return soup

def remove_non_ascii(text):
    return ''.join(i for i in text if ord(i)<128)
    #return s.encode('ascii', errors='ignore').decode()

def replace_non_ascii(text):
    return ''.join(i if ord(i)<128 else '*' for i in text)


###############################################################################
# SCRAPER
###############################################################################

class Scraper(object):
    def __init__(self, URL):
        self.soup = get_soup(URL)  # TODO: Error checking in case None


###############################################################################
# BUILDING SCRAPER
###############################################################################


class BuildingScraper(object):
    '''
    Gets Buildings off of Princeton Campus Map's listing, 
    some of these may be spelled differently on other sites (TO FIX)
    '''

    def __init__(self):
        BUILDINGS_URL = 'http://m.princeton.edu/map/category?group=princeton&feed=d4a5c388d8&_b=%5B%5D'
        self.soup = get_soup(BUILDINGS_URL)  # TODO: Error checking in case None

    def getBuildings(self):
        buildings = []
        soup = self.soup
        buildingsUL = soup.find('ul', class_="results")
        buildingsList = buildingsUL.findChildren()
        for listElement in buildingsList:
            buildingString = listElement.text.encode('utf-8')
            building = Building(buildingString)
            buildings.append(building)
        return buildings


###############################################################################
# DORM SCRAPER (Laundry, Kitchen)
###############################################################################


class DormScraper(object):
    def __init__(self):
        DORM_URL = 'https://hres.princeton.edu/undergraduates/resident-services/services-your-dormitory'
        self.soup = get_soup(DORM_URL)  # TODO: Error checking in case None

    def parseFacilityCollegeList_(self, facilityType, facilityCollegeList):
        facilities = []
        for listElement in facilityCollegeList:
            dormLocationString = listElement.text.encode('utf-8')
            dormLocationInfo = dormLocationString.split('-', 1)
            dormLocationInfo = [info.strip() for info in dormLocationInfo]
            building = dormLocationInfo[0]
            location = dormLocationInfo[1] if len(dormLocationInfo) > 1 else ""
            description = remove_non_ascii(location)
            #description = unicodedata.normalize('NFKD', location).encode('ascii','ignore')
            facility = Facility(facilityType, building, description)
            facilities.append(facility)
        return facilities


    def getKitchens(self):
        soup = self.soup
        
        kitchens = []

        resHeader = soup.find('h3', string='Kitchen Locations - Residential Colleges')
        resDiv = resHeader.parent
        resCollegeParagraphs = resDiv.find_all('p')
        
        for resCollegeParagraph in resCollegeParagraphs:
            college = resCollegeParagraph.text.encode('utf-8').strip(" College")
            resCollegeList = resCollegeParagraph.nextSibling
            kitchensResCollege = self.parseFacilityCollegeList_("kitchen", resCollegeList)
            kitchens.extend(kitchensResCollege)

        upperclassHeader = soup.find('h3', string='Kitchen Locations - Upperclass Dormitories')
        upperclassList = upperclassHeader.nextSibling.findChildren()
        kitchensUpperClass = self.parseFacilityCollegeList_("kitchen", upperclassList)
        kitchens.extend(kitchensUpperClass)

        return kitchens


    def getLaundries(self):
        soup = self.soup

        laundries = []
        
        resHeader = soup.find('h3', string='Laundry Locations - Residential Colleges')
        resDiv = resHeader.parent
        resCollegeParagraphs = resDiv.find_all('p')
        
        for resCollegeParagraph in resCollegeParagraphs:
            college = resCollegeParagraph.text.encode('utf-8').strip(" College")
            resCollegeList = resCollegeParagraph.nextSibling
            laundriesResCollege = self.parseFacilityCollegeList_("laundry", resCollegeList)
            laundries.extend(laundriesResCollege)

        upperclassHeader = soup.find('h3', string='Laundry Locations - Upperclass Dormitories')
        upperclassList = upperclassHeader.nextSibling.findChildren()
        laundriesUpperClass = self.parseFacilityCollegeList_("laundry", upperclassList)
        laundries.extend(laundriesUpperClass)

        return laundries


###############################################################################
# LIBRARY SCRAPER
###############################################################################

class LibraryScraper(object):
    def __init__(self):
        LIBRARY_URL = 'http://library.princeton.edu/libraries'
        self.soup = get_soup(LIBRARY_URL)


    def getLibraries(self, facility_type_name="library"):
        soup = self.soup
        
        libraries = []

        view_libraries = soup.find('div', class_='view-libraries')
        view_rows = view_libraries.find_all('div', class_='views-row')
        
        for row in view_rows:
            icon_location = row.find('span', class_='icon-location')
            building_name = icon_location.parent.find('a').text
            p = row.find('p')
            description = remove_non_ascii(p.text)
            library = Facility(facility_type_name, building_name, description)
            libraries.append(library)

        return libraries


###############################################################################
# PRINTER SCRAPER
###############################################################################

# TODO: Make other Scrapers follow class pattern

class PrinterScraper(object):

    def __init__(self):
        URL = 'https://princeton.service-now.com/kb_view.do?sysparm_article=KB0010348'
        self.soup = get_soup(URL)


    def getFacilities(self, facility_type_name="printer"):
        soup = self.soup
        
        facilities = []

        div_article = soup.find('div', id='article')
        ps = div_article.find_all('p')
        printer_p = ps[3]
        spans = printer_p.find_all('span')
        
        for span in spans:
            text = replace_non_ascii(span.text)
            texts = text.split('*')
            building_name = texts[0]
            description = texts[-1].lstrip()
            facility = Facility(facility_type_name, building_name, description)
            facilities.append(facility)

        return facilities


###############################################################################
# FOOD SCRAPER
###############################################################################

# TODO: Make other Scrapers follow class pattern

class FoodScraper(object):

    def __init__(self):
        URL = 'https://dining.princeton.edu/where-eat'
        soup = get_soup(URL)
        big_row = soup.find('div', class_='row')
        a_tags = big_row.find_all('a')
        url_tails = map(lambda a_tag: a_tag['href'], a_tags)
        self.url_tails = set(url_tails)
        

    def getFacilities(self, facility_type_name="food"):
        url_head = "https://dining.princeton.edu"
        url_tails = self.url_tails
        facilities = []
        for url_tail in url_tails:
            url = "{}{}".format(url_head, url_tail)
            soup = get_soup(url)
            try:
                facility = self.getFacility(soup)
                facilities.append(facility)
            except:
                continue
        return facilities


    def getFacility(self, soup, facility_type_name="food"):
        facility = None
        
        h1 = soup.find('h1', id='page-title')
        building_name = h1.text

        div = soup.find('div', class_='field-type-text-with-summary')
        description = remove_non_ascii(div.text)
        
        iframe = soup.find('iframe')
        iframe_url = iframe['src']
        latlon_start = iframe_url.find('q=') + 2
        latlon_str = iframe_url[latlon_start:].split('&')[0]
        latlon_arr = latlon_str.split('%2C')
        try:
            lat = latlon_arr[0]
            lon = latlon_arr[1].lstrip('+')
            lat, lon = float(lat), float(lon)
        except:
            lat, lon = None, None

        facility = Facility(facility_type_name, building_name, description, lat, lon)
        return facility


###############################################################################
# Helper Methods to add Scraper data to Database
###############################################################################


def has_facility_type(facility_type_name):
    db = Database()
    ft = db.getFacilityType(facility_type_name)
    return ft != None


def add_kitchen_and_laundry():

    db = Database(debug=False)
    dormScraper = DormScraper()

    ft_name = "kitchen"
    if has_facility_type(ft_name):
        print "{} type exists".format(ft_name)
        return
    else:
        db.addFacilityType(ft)
    kitchens = dormScraper.getKitchens()
    db.setFacilitiesLatLon(kitchens)
    db.addFacilities(kitchens)

    ft_name = "laundry"
    if has_facility_type(ft_name):
        print "{} type exists".format(ft_name)
        return
    else:
        db.addFacilityType(ft)
    laundries = dormScraper.getLaundries()
    db.setFacilitiesLatLon(laundries)
    db.addFacilities(laundries)


def add_libraries():
    ft_name = "library"
    verify_facility_type(ft_name)
    db = Database()
    if has_facility_type(ft_name):
        print "{} type exists".format(ft_name)
        return
    else:
        db.addFacilityType(ft)
    scraper = LibraryScraper()
    libraries = scraper.getLibraries(facility_type_name=ft_name)
    db.setFacilitiesLatLon(libraries)
    db.addFacilities(libraries)


def add_printers():
    ft_name = "printer"
    ft = FacilityType(ft_name)
    db = Database()
    if has_facility_type(ft_name):
        print "{} type exists".format(ft_name)
        return
    else:
        db.addFacilityType(ft)
    scraper = PrinterScraper()
    facilities = scraper.getFacilities(facility_type_name=ft_name)
    db.setFacilitiesLatLon(facilities)
    db.addFacilities(facilities)


def add_food():
    ft_name = "food"
    ft = FacilityType(ft_name)
    db = Database()
    if has_facility_type(ft_name):
        print "{} type exists".format(ft_name)
        #return
    else:
        db.addFacilityType(ft)
    scraper = FoodScraper()
    facilities = scraper.getFacilities(facility_type_name=ft_name)
    #for f in facilities: print f.toDict()
    #db.setFacilitiesLatLon(facilities)
    db.addFacilities(facilities)


def test():
    #db = Database(debug=False)
    #kitchens = db.getFacilitiesFromFacilityType("kitchen")
    pass


if __name__ == '__main__':
    '''
    TODOS: 
    Add Res Colleges and their Lat, Lons
    Update Lat, Lons for facilities with null vals
    Fix Printer Lat, Lons
    '''
    test()
