mvn clean install -DskipTests
source venv/bin/activate
python3 -m build -o /home/gomes/code/otm/otm-sim/target/
deactivate
