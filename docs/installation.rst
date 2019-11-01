Building OTM
============

You can obtain OTM as a jar file `here <https://mymavenrepo.com/repo/XtcMAROnIu3PyiMCmbdY/otm/otm-sim/1.0-SNAPSHOT/>`_. Just download the most recent file of the form ``otm-sim-1.0-<date>.<time>-<number>-jar-with-dependencies.jar``.

Follow these steps if you wish to build OTM from code.

1. Clone the `code <https://github.com/ggomes/otm-sim>`_::
	
	git clone https://github.com/ggomes/otm-sim.git

2. Install `Apache Maven <https://maven.apache.org/install.html>`_
	
3. Add `OTM's Maven repository profile <https://github.com/ggomes/otm-sim/blob/master/settings.xml>`_ to your Maven settings file: ``~/.m2/settings.xml``. If you do not already have `~/.m2/settings.xml`_ then you can use the one provided.

4. Build::

	mvn install -DskipTests