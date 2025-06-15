### Basic (Grundlegend)
Ein grundlegender Beispiel-Smart-Contract, der die Erstellung und Übertragung eines Assets durch Schreiben und Abrufen von Daten aus dem Ledger ermöglicht. Dieses Beispiel ist für neue Fabric-Benutzer empfehlenswert.
* **Anleitung:** Schreiben der ersten Anwendung
* **Sprachen:** Java
* **Template:** [Beispiel](./templates/transfer/basic/)

-----

### Private Daten
Dieses Beispiel demonstriert die Verwendung von privaten Datenkollektionen, wie diese mit dem Chaincode-Lifecycle verwaltet werden und wie der Hash privater Daten zur Verifizierung auf dem Ledger verwendet werden kann. Es zeigt auch, wie Asset-Aktualisierungen und -Übertragungen durch clientbasierten Besitz und Zugriffskontrolle gesteuert werden.
* **Anleitung:** Verwendung von Privaten Daten
* **Sprachen:** Java
* **Template:** [Beispiel](./templates/transfer/private-data/)

-----

### Zustandsbasierte Bestätigungsrichtlinien (State-Based Endorsement)
Dieses Beispiel zeigt, wie die Bestätigungsrichtlinie auf Chaincode-Ebene überschrieben werden kann, um Richtlinien auf Schlüssel-Ebene (Daten-/Asset-Ebene) festzulegen.
* **Anleitung:** Verwendung von zustandsbasierten Bestätigungsrichtlinien
* **Sprachen:** Java
* **Template:** [Beispiel](./templates/transfer/sbe/)

-----

### Ereignisse (Events)
Das Beispiel für Ereignisse zeigt, wie Smart Contracts Ereignisse auslösen können, die von den Anwendungen, die mit dem Netzwerk interagieren, gelesen werden.
* **Anleitung:** README
* **Sprachen:** Java
* **Template:** [Beispiel](./templates/transfer/events/)

-----

### Gesicherte Vereinbarung
Ein Smart Contract, der implizite private Datenkollektionen, zustandsbasierte Bestätigungsrichtlinien sowie organisationsbasierten Besitz und Zugriffskontrolle verwendet, um Daten privat zu halten und ein Asset sicher mit Zustimmung des Besitzers und des Käufers zu übertragen.
* **Anleitung:** Gesicherte Asset-Übertragung
* **Sprachen:** Go

-----

### Attributbasierte Zugriffskontrolle
Demonstriert die Verwendung von attribut- und identitätsbasierter Zugriffskontrolle anhand eines einfachen Asset-Übertragungsszenarios.
* **Anleitung:** README
* **Sprachen:** Go

-----

### Ledger-Abfragen
Das Beispiel für Ledger-Abfragen demonstriert Bereichsabfragen (anwendbar für LevelDB und CouchDB) und wie mit dem Chaincode ein Index bereitgestellt wird, um JSON-Abfragen zu unterstützen (nur für CouchDB anwendbar).
* **Anleitung:** Verwendung von CouchDB
* **Sprachen:** go