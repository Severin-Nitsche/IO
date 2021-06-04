project:
	find src -name "*.java" | xargs javac --release 15 -d out/production --module-source-path src

rebuild:
	mkdir -p out/artifacts
	jar -cf out/artifacts/IO.jar -C out/production/com.github.severinnitsche.io .

build: project rebuild
