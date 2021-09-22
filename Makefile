all: build import export package-json package-db

build:
	mvn clean compile

import:
	mvn exec:java -Dexec.mainClass="pe.gob.congreso.pl.Import"

export:
	mvn exec:java -Dexec.mainClass="pe.gob.congreso.pl.Export"

package-json:
	tar -czvf proyectos-ley-2021-json.tar.gz proyectos-ley-*.json

package-db:
	tar -czvf proyectos-ley-2021-db.tar.gz proyectos-ley-*.db
