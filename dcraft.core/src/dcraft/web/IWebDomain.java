package dcraft.web;

import dcraft.web.http.SslContextFactory;

public interface IWebDomain {

	SslContextFactory getSecureContextFactory(String hostname);
}
