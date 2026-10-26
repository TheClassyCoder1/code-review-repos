"""DNS-01 helpers for issuing certificates on the partner-facing domains."""

import dns.message
import dns.name
import dns.query
import dns.rcode
import dns.rdatatype
import dns.resolver
from flask import current_app

from opsdesk.part2.metrics import metrics


def get_authoritative_nameserver(domain):
    if current_app.config.get("ACME_DYN_GET_AUTHORATATIVE_NAMESERVER"):
        n = dns.name.from_text(domain)

        depth = 2
        default = dns.resolver.get_default_resolver()
        nameserver = default.nameservers[0]

        last = False
        while not last:
            s = n.split(depth)

            last = s[0].to_unicode() == "@"
            sub = s[1]

            query = dns.message.make_query(sub, dns.rdatatype.NS)
            response = dns.query.udp(query, nameserver)

            rcode = response.rcode()
            if rcode != dns.rcode.NOERROR:
                metrics.send("get_authoritative_nameserver_error", "counter", 1)
                if rcode == dns.rcode.NXDOMAIN:
                    raise Exception("%s does not exist." % sub)
                else:
                    raise Exception("Error %s" % dns.rcode.to_text(rcode))

            if len(response.authority) > 0:
                rrset = response.authority[0]
            else:
                rrset = response.answer[0]

            rr = rrset[0]
            if rr.rdtype != dns.rdatatype.SOA:
                authority = rr.target
                nameserver = default.query(authority).rrset[0].to_text()

            depth += 1

        return nameserver
    else:
        return "8.8.8.8"
