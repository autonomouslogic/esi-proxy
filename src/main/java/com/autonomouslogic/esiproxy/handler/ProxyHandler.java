package com.autonomouslogic.esiproxy.handler;

import com.autonomouslogic.esiproxy.EsiRelay;
import io.helidon.webserver.http.Handler;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.log4j.Log4j2;

@Singleton
@Log4j2
public class ProxyHandler implements Handler {
	@Inject
	protected EsiRelay esiRelay;

	@Inject
	protected ProxyHandler() {}

	@Override
	public void handle(ServerRequest req, ServerResponse res) throws Exception {
		log.info("Got request: {}", req.path());
		var response = esiRelay.request(req);
	}
}
