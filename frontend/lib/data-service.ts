// Simulierte REST API für Demo-Zwecke
// In Zukunft wird dies durch echte API-Calls ersetzt

type JsonData = Record<string, any>

const demoData: Record<string, JsonData> = {
  "MED-1": {
    identifikation: {
      medikamenten_id: "MED-1",
      pzn: "08296572",
      name: "Aspirin 500mg",
      handelsname: "Aspirin® 500mg Tabletten",
      hersteller: "Bayer AG",
      hersteller_referenz: "HERSTELLER-1",
      zulassungsnummer: "DE-12345-67890",
      produktwebsite: "https://www.aspirin.de/produkte/aspirin-500mg",
      fachinformation: "https://www.fachinfo.de/database/fi/aspirin-500mg.pdf",
    },
    wirkstoff: {
      hauptwirkstoff: "Acetylsalicylsäure",
      cas_nummer: "50-78-2",
      menge_pro_einheit: "500mg",
      reinheit: "99.5%",
      herkunft: "Synthetisch",
      molekulargewicht: "180.16 g/mol",
      sicherheitsdatenblatt: "https://www.bayer.com/sites/default/files/2020-02/sdb-acetylsalicylsaeure.pdf",
      pubchem_url: "https://pubchem.ncbi.nlm.nih.gov/compound/2244",
    },
    darreichungsform: {
      form: "Tabletten",
      farbe: "Weiß",
      groesse: "12mm Durchmesser",
      gewicht_pro_tablette: "650mg",
      teilbarkeit: true,
      beschichtung: "Filmtablette",
    },
    verpackung: {
      packungsgroesse: 20,
      einheit: "Stück",
      verpackungsart: "Blister",
      beipackzettel: true,
      kindersicherung: false,
      lichtschutz: true,
      verknuepfte_units: ["UNIT-1"],
    },
    haltbarkeit: {
      verfallsdatum: "12/2026",
      haltbarkeitsdauer: "36 Monate",
      lagerungsbedingungen: {
        temperatur: "unter 25°C",
        luftfeuchtigkeit: "unter 60%",
        lichtschutz: "vor direktem Licht schützen",
        besondere_hinweise: "Trocken lagern",
        lagerungsrichtlinien: "https://www.bayer.com/de/lagerung-arzneimittel",
      },
    },
    anwendung: {
      indikationen: ["Leichte bis mäßige Schmerzen", "Fieber", "Entzündungen", "Kopfschmerzen", "Zahnschmerzen"],
      dosierung: {
        erwachsene: "1-2 Tabletten alle 4-6 Stunden",
        max_tagesdosis: "6 Tabletten (3000mg)",
        kinder_ab_12: "1 Tablette alle 6 Stunden",
        kinder_unter_12: "Nicht empfohlen",
        dosierungsrechner: "https://www.aspirin.de/dosierung-rechner",
      },
      kontraindikationen: [
        "Allergie gegen Salicylate",
        "Schwere Nierenfunktionsstörung",
        "Schwere Leberfunktionsstörung",
        "Schwangerschaft (3. Trimester)",
        "Stillzeit",
        "Magen-Darm-Geschwüre",
      ],
      nebenwirkungen: {
        haeufig: ["Magenbeschwerden", "Übelkeit"],
        gelegentlich: ["Kopfschmerzen", "Schwindel"],
        selten: ["Allergische Reaktionen", "Hautausschlag"],
        meldung_nebenwirkungen: "https://www.bfarm.de/nebenwirkungen-melden",
      },
      wechselwirkungen_info: "https://www.gelbe-liste.de/wechselwirkungen/aspirin",
    },
    regulatorische_daten: {
      zulassungsinhaber: "Bayer Vital GmbH",
      zulassungsdatum: "15.03.2018",
      letzte_aenderung: "22.11.2023",
      ema_produktinfo: "https://www.ema.europa.eu/en/medicines/human/EPAR/aspirin",
      pharmakovigilanz: {
        meldungen_2023: 12,
        schwerwiegende_ereignisse: 2,
        letzter_bericht: "31.12.2023",
        kontakt_pharmakovigilanz: "https://www.bayer.com/de/pharmakovigilanz-kontakt",
      },
    },
    // Vereinfachte Meta-Popup Beispiele - Key-Value Format
    _meta_popup_warnung: "Dieses Medikament sollte nicht mit Alkohol eingenommen werden.",
    _meta_popup_hinweis: {
      author: "Pharmakovigilanz",
      message: "Neue Sicherheitsinformationen verfügbar. Bitte Fachinformation beachten.",
      type: "info",
      priority: 1,
    },
    _meta_popup_qualitaet: {
      author: "QS-Team",
      message: "Charge wurde erfolgreich freigegeben. Alle Tests bestanden.",
      type: "success",
      priority: 0,
    },
    // Neue Lieferketten-Daten
    _meta_lieferkette_produktion: {
      title: "Produktions-Lieferkette",
      "Dr. Schmidt": "2024-07-01T14:32:00Z",
      "QS-Team": "2024-07-01T15:45:00Z",
      Verpackung: "2024-07-01T16:00:00Z",
    },
    _meta_lieferkette_distribution: {
      title: "Distributions-Lieferkette",
      "Lager Köln": "2024-07-01T18:00:00Z",
      "Transport DHL": "2024-07-01T20:30:00Z",
      "Hub Berlin": "2024-07-02T06:15:00Z",
      Zustellung: "2024-07-02T08:30:00Z",
    },
  },

  "HERSTELLER-1": {
    unternehmensdaten: {
      hersteller_id: "HERSTELLER-1",
      firmenname: "Bayer AG",
      rechtsform: "Aktiengesellschaft",
      hauptsitz: "Leverkusen, Deutschland",
      gruendungsjahr: 1863,
      mitarbeiterzahl: 99538,
      jahresumsatz: "50.7 Milliarden EUR",
      boersensymbol: "BAYN",
      unternehmenswebsite: "https://www.bayer.com",
      investor_relations: "https://www.bayer.com/de/investors",
      nachhaltigkeitsbericht: "https://www.bayer.com/sites/default/files/2023-03/bayer-nachhaltigkeitsbericht-2022.pdf",
      verknuepfte_medikamente: ["MED-1"],
      produzierte_units: ["UNIT-1"],
    },
    produktionsstandorte: [
      {
        standort_id: "DE-LEV-001",
        name: "Werk Leverkusen",
        adresse: "Kaiser-Wilhelm-Allee 1, 51368 Leverkusen",
        produktionskapazitaet: "2.5 Millionen Einheiten/Monat",
        spezialisierung: ["Tabletten", "Kapseln", "Injektionslösungen"],
        mitarbeiter: 8500,
        inbetriebnahme: "1891",
        standort_website: "https://www.bayer.com/de/standorte/leverkusen",
        virtual_tour: "https://www.bayer.com/de/virtual-tour/leverkusen",
        produzierte_medikamente: ["MED-1"],
      },
      {
        standort_id: "DE-BER-002",
        name: "Werk Berlin",
        adresse: "Müllerstraße 178, 13353 Berlin",
        produktionskapazitaet: "1.8 Millionen Einheiten/Monat",
        spezialisierung: ["Salben", "Cremes", "Tropfen"],
        mitarbeiter: 3200,
        inbetriebnahme: "1936",
        standort_website: "https://www.bayer.com/de/standorte/berlin",
      },
    ],
    qualitaetsmanagement: {
      gmp_zertifizierung: {
        nummer: "DE-NW-05-MIA-2024",
        gueltig_bis: "31.12.2025",
        aussteller: "Regierung von Düsseldorf",
        letzte_inspektion: "15.08.2024",
        naechste_inspektion: "15.08.2025",
        zertifikat_download: "https://www.regierung.nrw.de/gmp-zertifikate/bayer-ag-2024.pdf",
        inspektionsbericht: "https://www.regierung.nrw.de/inspektionen/bayer-leverkusen-2024.pdf",
      },
      iso_zertifizierungen: [
        {
          standard: "ISO 9001:2015",
          bereich: "Qualitätsmanagement",
          gueltig_bis: "22.06.2026",
          zertifikat_url: "https://certificates.tuv.com/bayer/iso-9001-2024.pdf",
        },
        {
          standard: "ISO 14001:2015",
          bereich: "Umweltmanagement",
          gueltig_bis: "22.06.2026",
          zertifikat_url: "https://certificates.tuv.com/bayer/iso-14001-2024.pdf",
        },
        {
          standard: "ISO 45001:2018",
          bereich: "Arbeitsschutzmanagement",
          gueltig_bis: "22.06.2026",
          zertifikat_url: "https://certificates.tuv.com/bayer/iso-45001-2024.pdf",
        },
      ],
      qualitaetskontrolle: {
        pruefungen_pro_charge: 15,
        durchschnittliche_pruefzeit: "48 Stunden",
        bestehensquote: "99.7%",
        labor_akkreditierung: "DAkkS D-PL-11234-01-00",
        labor_website: "https://www.bayer.com/de/qualitaetslabor",
        pruefverfahren_dokumentation:
          "https://www.bayer.com/sites/default/files/2023-01/qualitaetskontrolle-verfahren.pdf",
      },
    },
    personal: {
      fuehrungskrafte: [
        {
          position: "Produktionsleiter",
          name: "Dr. Michael Schmidt",
          qualifikation: "Pharmazie PhD, MBA",
          erfahrung_jahre: 15,
          verantwortlichkeiten: ["Produktionsplanung", "Qualitätssicherung"],
          linkedin_profil: "https://www.linkedin.com/in/michael-schmidt-pharma",
        },
        {
          position: "Qualitätsmanager",
          name: "Dr. Sarah Müller",
          qualifikation: "Qualified Person (QP), Chemie PhD",
          erfahrung_jahre: 12,
          verantwortlichkeiten: ["Chargenfreigabe", "Regulatorische Compliance"],
          xing_profil: "https://www.xing.com/profile/Sarah_Mueller_QP",
        },
      ],
      schulungen: {
        gmp_schulungen_2024: 1250,
        compliance_schulungen: 890,
        sicherheitsschulungen: 2100,
        durchschnittliche_schulungsstunden_pro_mitarbeiter: 32,
        schulungsportal: "https://learning.bayer.com/gmp-training",
        zertifizierungsprogramm: "https://www.bayer.com/de/karriere/weiterbildung",
      },
    },
    compliance: {
      regulatorische_inspektionen: {
        letzte_fda_inspektion: "12.03.2024",
        ergebnis: "No Action Indicated (NAI)",
        fda_bericht: "https://www.fda.gov/inspections/bayer-leverkusen-2024.pdf",
        letzte_ema_inspektion: "08.11.2023",
        ergebnis: "Compliant",
        ema_bericht: "https://www.ema.europa.eu/inspections/bayer-2023-report.pdf",
      },
      meldepflichten: {
        adverse_events_2024: 45,
        quality_defects_2024: 8,
        recalls_2024: 0,
        meldungsportal: "https://www.bayer.com/de/pharmakovigilanz/meldungen",
      },
      transparenz: {
        clinical_trials_register: "https://clinicaltrials.bayer.com",
        payment_transparency: "https://www.bayer.com/de/transparenz/zahlungen-hcp",
      },
    },
    // Vereinfachte Meta-Popup für Hersteller
    _meta_popup_compliance: {
      author: "Compliance-Team",
      message: "Alle regulatorischen Anforderungen erfüllt. Letzte Inspektion erfolgreich.",
      type: "success",
    },
    // Lieferketten für Hersteller
    _meta_lieferkette_rohstoffe: {
      title: "Rohstoff-Lieferkette",
      "Lieferant A": "2024-06-28T10:00:00Z",
      Qualitätsprüfung: "2024-06-28T14:30:00Z",
      Freigabe: "2024-06-29T09:00:00Z",
    },
  },

  "UNIT-1": {
    unit_identifikation: {
      unit_id: "UNIT-1",
      serialnummer: "2024070115432",
      batch_id: "BATCH-MED1-240701",
      produktions_id: "PROD-LEV-240701-001",
      herstellungsdatum: "01.07.2024",
      herstellungszeit: "14:32:15",
      medikament_referenz: "MED-1",
      hersteller_referenz: "HERSTELLER-1",
      qr_code_url: "https://qr.pharmalink.com/UNIT-1",
    },
    physische_eigenschaften: {
      verpackung: {
        typ: "Blisterpackung",
        material: "PVC/Aluminium",
        dicke_pvc: "0.25mm",
        dicke_aluminium: "0.02mm",
        abmessungen: {
          laenge: "125mm",
          breite: "85mm",
          hoehe: "18mm",
        },
        bruttogewicht: "47.5g",
        nettogewicht: "13.0g",
        anzahl_tabletten: 20,
        recycling_info: "https://www.bayer.com/de/nachhaltigkeit/verpackung-recycling",
      },
      qualitaetspruefung: {
        visuell: "Bestanden",
        gewichtskontrolle: "Bestanden (47.3g - Toleranz: ±2g)",
        dichtheitspruefung: "Bestanden",
        aufdruck_lesbarkeit: "Bestanden",
        pruefzeitpunkt: "01.07.2024 15:45:22",
        pruefer: "QC-Mitarbeiter-ID-0847",
        pruefprotokoll: "https://quality.bayer.com/protocols/UNIT-1-qc-2024.pdf",
        pruefgeraete_kalibrierung: "https://calibration.bayer.com/devices/qc-station-047.pdf",
      },
    },
    umgebungsbedingungen: {
      aktuelle_bedingungen: {
        temperatur: "22.3°C",
        luftfeuchtigkeit: "43%",
        luftdruck: "1013.2 hPa",
        letzter_check: "01.07.2024 16:30:00",
        sensor_id: "TEMP-SENS-001",
        monitoring_dashboard: "https://monitoring.pharmalink.com/units/UNIT-1/environment",
        sensor_kalibrierung: "https://calibration.pharmalink.com/sensors/TEMP-SENS-001",
      },
      sollwerte: {
        temperatur_min: "15°C",
        temperatur_max: "25°C",
        luftfeuchtigkeit_max: "60%",
        kritische_temperatur: "30°C",
        alarm_einstellungen: "https://monitoring.pharmalink.com/alarms/UNIT-1/config",
      },
      verlauf_24h: [
        { zeitpunkt: "00:00", temperatur: "21.8°C", luftfeuchtigkeit: "45%" },
        { zeitpunkt: "06:00", temperatur: "21.2°C", luftfeuchtigkeit: "48%" },
        { zeitpunkt: "12:00", temperatur: "22.5°C", luftfeuchtigkeit: "41%" },
        { zeitpunkt: "18:00", temperatur: "22.3°C", luftfeuchtigkeit: "43%" },
      ],
      historische_daten: "https://monitoring.pharmalink.com/units/UNIT-1/history",
    },
    lieferkette_tracking: {
      aktueller_status: "Im Transit",
      aktuelle_position: {
        latitude: 52.52,
        longitude: 13.405,
        ort: "Berlin, Deutschland",
        genauigkeit: "±5m",
        letztes_update: "01.07.2024 16:15:33",
        live_tracking: "https://tracking.pharmalink.com/live/UNIT-1",
        karte_anzeigen: "https://maps.google.com/maps?q=52.52,13.405",
      },
      transport_details: {
        fahrzeug_id: "DHL-TRUCK-7834",
        fahrer: {
          name: "Max Mustermann",
          fuehrerschein: "DE-12345678",
          kontakt: "+49-123-456789",
          fahrer_profil: "https://drivers.dhl.com/profile/max-mustermann",
        },
        route: "Leverkusen → Berlin → Hamburg",
        geplante_ankunft: "02.07.2024 10:00:00",
        geschaetzte_ankunft: "02.07.2024 09:45:00",
        dhl_tracking: "https://www.dhl.de/de/privatkunden/pakete-empfangen/verfolgen.html?piececode=DHL-TRUCK-7834",
        route_optimierung: "https://logistics.dhl.com/route-optimizer/UNIT-1",
      },
      naechster_checkpoint: {
        name: "Adler-Apotheke Berlin",
        adresse: "Hauptstraße 123, 10115 Berlin",
        kontakt: "+49-30-123456",
        oeffnungszeiten: "Mo-Fr: 8:00-20:00, Sa: 9:00-16:00",
        ansprechpartner: "Apotheker Dr. Weber",
        apotheken_website: "https://www.adler-apotheke-berlin.de",
        google_maps: "https://maps.google.com/maps?q=Hauptstraße+123,+10115+Berlin",
      },
    },
    lieferkette_historie: [
      {
        station: "Produktion",
        ort: "Bayer Werk Leverkusen",
        adresse: "Kaiser-Wilhelm-Allee 1, 51368 Leverkusen",
        datum: "01.07.2024",
        zeit: "16:00:00",
        status: "Abgeschlossen",
        verantwortlich: "Dr. Schmidt (Produktionsleiter)",
        temperatur: "23.1°C",
        luftfeuchtigkeit: "42%",
        aktivitaeten: ["Verpackung", "Qualitätskontrolle", "Etikettierung"],
        hersteller_referenz: "HERSTELLER-1",
        produktionsbericht: "https://production.bayer.com/reports/UNIT-1-production.pdf",
      },
      {
        station: "Versandzentrum",
        ort: "DHL Logistikzentrum Köln",
        adresse: "Wahlerstraße 28, 40472 Düsseldorf",
        datum: "01.07.2024",
        zeit: "20:30:00",
        status: "Abgeschlossen",
        verantwortlich: "Logistik-Team-ID-4521",
        temperatur: "20.5°C",
        luftfeuchtigkeit: "48%",
        aktivitaeten: ["Eingang", "Sortierung", "Verladung"],
        dhl_standort: "https://www.dhl.de/de/geschaeftskunden/paket/information/geschaeftsstellen-finden.html",
      },
      {
        station: "Transport Hub",
        ort: "DHL Hub Berlin",
        adresse: "Flughafen Berlin Brandenburg",
        datum: "02.07.2024",
        zeit: "06:15:00",
        status: "Abgeschlossen",
        verantwortlich: "Hub-Manager-ID-7789",
        temperatur: "19.8°C",
        luftfeuchtigkeit: "52%",
        aktivitaeten: ["Umschlag", "Qualitätskontrolle", "Weiterleitung"],
        hub_info: "https://www.dhl.de/de/geschaeftskunden/paket/information/dhl-hubs.html",
      },
      {
        station: "Auslieferung",
        ort: "Zustellfahrzeug DHL-7834",
        adresse: "Unterwegs nach Berlin Mitte",
        datum: "02.07.2024",
        zeit: "08:30:00",
        status: "In Bearbeitung",
        verantwortlich: "Fahrer Max Mustermann",
        temperatur: "22.3°C",
        luftfeuchtigkeit: "43%",
        geschaetzte_ankunft: "02.07.2024 09:45:00",
        live_tracking: "https://tracking.dhl.com/live/DHL-7834",
      },
    ],
    compliance_dokumentation: [
      {
        dokumenttyp: "Beipackzettel",
        dateiname: "Beipackzettel_MED-1_v2.1.pdf",
        groesse: "1.2 MB",
        sprachen: ["Deutsch", "Englisch", "Französisch"],
        version: "v2.1",
        erstellt_am: "15.06.2024",
        gueltig_bis: "15.06.2027",
        download_url: "https://docs.bayer.com/beipackzettel/MED-1-v2.1.pdf",
        medikament_referenz: "MED-1",
      },
      {
        dokumenttyp: "Konformitätszertifikat",
        dateiname: "CE_Konformitaet_UNIT-1.pdf",
        groesse: "850 KB",
        aussteller: "TÜV Rheinland",
        zertifikat_nummer: "TUV-CERT-2024-7891",
        gueltig_bis: "31.12.2025",
        pruefnormen: ["EN ISO 13485", "MDR 2017/745"],
        download_url: "https://certificates.tuv.com/pharma/UNIT-1-conformity.pdf",
        verifikation_url: "https://www.tuv.com/verify/TUV-CERT-2024-7891",
      },
      {
        dokumenttyp: "Sicherheitsdatenblatt",
        dateiname: "SDB_Aspirin_500mg_v1.3.pdf",
        groesse: "2.5 MB",
        version: "v1.3",
        letzte_aktualisierung: "01.01.2024",
        naechste_ueberpruefung: "01.01.2025",
        gefahrenklassen: ["Keine besonderen Gefahren"],
        download_url: "https://safety.bayer.com/sdb/aspirin-500mg-v1.3.pdf",
        medikament_referenz: "MED-1",
        hersteller_referenz: "HERSTELLER-1",
      },
    ],
    // Meta-Popup für Unit
    _meta_popup_transport: {
      author: "Logistik",
      message: "Transport läuft planmäßig. Ankunft voraussichtlich 09:45 Uhr.",
      type: "info",
    },
    // Lieferketten für Unit
    _meta_lieferkette_transport: {
      title: "Transport-Lieferkette",
      Versandzentrum: "2024-07-01T20:30:00Z",
      "Hub Berlin": "2024-07-02T06:15:00Z",
      Zustellfahrzeug: "2024-07-02T08:30:00Z",
      Endkunde: "2024-07-02T09:45:00Z",
    },
  },
}

// -----------------------------------------------------------------------------
//  ↓↓↓ REST OF ORIGINAL LOGIC (restored) ↓↓↓
// -----------------------------------------------------------------------------

export interface DataServiceResponse {
  data: JsonData | null
  error: string | null
  source: "local" | "api"
  timestamp: string
  cached?: boolean
  correctedId?: string
  linkedIds?: {
    medikament?: string
    hersteller?: string
    unit?: string[]
  }
}

// Funktionen zum Extrahieren verknüpfter IDs / Pop-ups usw.
import { getDataSource, getConfig } from "./config"
import { apiClient } from "./api-client"

// Hilfsfunktion – verknüpfte IDs ermitteln (unverändert)
function extractLinkedIds(data: JsonData): {
  medikament?: string
  hersteller?: string
  unit?: string[]
} {
  const linkedIds: { medikament?: string; hersteller?: string; unit?: string[] } = {}

  function search(obj: any): void {
    if (typeof obj === "string") {
      if (/^MED-/.test(obj) && !linkedIds.medikament) linkedIds.medikament = obj
      else if (/^HERSTELLER-/.test(obj) && !linkedIds.hersteller) linkedIds.hersteller = obj
      else if (/^UNIT-/.test(obj)) {
        linkedIds.unit ??= []
        if (!linkedIds.unit.includes(obj)) linkedIds.unit.push(obj)
      }
    } else if (Array.isArray(obj)) obj.forEach(search)
    else if (obj && typeof obj === "object") Object.values(obj).forEach(search)
  }

  search(data)
  return linkedIds
}

// --------------------------------------------
//  getDataById  (identisch zur vorherigen Version)
// --------------------------------------------
export async function getDataById(id: string): Promise<DataServiceResponse> {
  const dataSource = getDataSource()
  const config = getConfig()

  if (dataSource === "local") {
    // Local logic bleibt gleich
    let actualId = id
    let data = demoData[id]

    if (!data) {
      const similar = Object.keys(demoData).find((k) => k.toLowerCase().includes(id.toLowerCase()))
      if (similar) {
        actualId = similar
        data = demoData[similar]
      }
    }

    const linkedIds = data ? extractLinkedIds(data) : undefined

    return {
      data,
      error: data ? null : `ID "${id}" not found in local data`,
      source: "local",
      timestamp: new Date().toISOString(),
      correctedId: actualId !== id ? actualId : undefined,
      linkedIds,
    }
  }

  // API MODE - verwende einheitliche Search-URL
  try {
    const response = await apiClient.searchData(id)

    if (response.error) {
      return {
        data: null,
        error: response.error,
        source: "api",
        timestamp: response.timestamp,
      }
    }

    const apiData = response.data
    const correctedId = apiData?.correctedId || apiData?.corrected_id
    const linkedIds = apiData ? extractLinkedIds(apiData) : undefined

    return {
      data: apiData,
      error: null,
      source: "api",
      timestamp: response.timestamp,
      cached: response.source === "cache",
      correctedId: correctedId !== id ? correctedId : undefined,
      linkedIds,
    }
  } catch (err) {
    return {
      data: null,
      error: err instanceof Error ? err.message : "Unknown API error",
      source: "api",
      timestamp: new Date().toISOString(),
    }
  }
}

// --------------------------------------------
//  Simple helpers (unchanged)
// --------------------------------------------
export function isValidId(id: string): boolean {
  const dataSource = getDataSource()

  if (dataSource === "local") return id in demoData
  return /^(MED-|HERSTELLER-|UNIT-).+/.test(id)
}

export function getAvailableIds(): string[] {
  return Object.keys(demoData)
}
