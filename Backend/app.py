from flask import Flask, request, jsonify
import sqlite3
from database import Database

app = Flask(__name__)
db = Database()

@app.route('/')
def homepage():
    argslist = request.args
    faciltype = argslist.get('facil')
    facils = []
    try:
        facils = db.getFacilitiesFromFacilityType(faciltype)
        facils = map(lambda facil: facil.toDictNoType(), facils)
        # https://stackoverflow.com/questions/5022066/how-to-serialize-sqlalchemy-result-to-json
    except:
        facils = []
    return jsonify(facilities=facils)


@app.route('/auto')
def autocomplete():
    argslist = request.args
    terms = []
    try:
        term = argslist.get('term')
        if len(term) > 0:
            terms = db.getBuildingsLike(term)
            terms = map(lambda bldg: bldg.toDictNoLatLon(), terms)
    except:
        terms = []
    return jsonify(terms=terms)


@app.route('/building-location')
def getBuilding():
    argslist = request.args
    bldg = {}
    try:
        bldg = argslist.get('name')
        bldg = db.getBuilding(bldg)
        bldg = bldg.toDict()
    except:
        bldg = {}
    return jsonify(building=bldg)

@app.route('/building')
def getFacilitiesFromBuilding():
    argslist = request.args
    facils = []
    try:
        bldg = argslist.get('name')
        facils = db.getFacilitiesFromBuilding(bldg)
        facils = map(lambda facil: facil.toDict(), facils)
    except:
        facils = []
    return jsonify(facilities=facils)


@app.route('/faciltypes')
def getFacilityTypes():
    argslist = request.args
    faciltypes = []
    try:
        faciltypes = db.getAllFacilityTypes()
        faciltypes = map(lambda faciltype: faciltype.name, faciltypes)
    except:
        faciltypes = []
    return jsonify(facility_types=faciltypes)


if __name__ == '__main__':
    app.run(debug=True, use_reloader=True)
