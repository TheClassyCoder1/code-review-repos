"""Mailbox intake for the support helpdesk queues, overriding django-helpdesk's `process_queue`."""

import imaplib
import os
import poplib
import socket
from os.path import isfile, join
from time import ctime

from django.utils import encoding
from helpdesk import settings as helpdesk_settings
from helpdesk.email import (DeleteIgnoredTicketException, IgnoreTicketException, extract_email_metadata,
                            imap_oauth_sync, imap_sync, pop3_sync)


def process_queue(q, logger):
    logger.info(
        f"***** {ctime()}: Begin processing mail for django-helpdesk queue: {q.title}"
    )

    if q.socks_proxy_type and q.socks_proxy_host and q.socks_proxy_port:
        try:
            import socks
        except ImportError:
            no_socks_msg = (
                "Queue has been configured with proxy settings, "
                "but no socks library was installed. Try to "
                "install PySocks via PyPI."
            )
            logger.error(no_socks_msg)
            raise ImportError(no_socks_msg)

        proxy_type = {
            "socks4": socks.SOCKS4,
            "socks5": socks.SOCKS5,
        }.get(q.socks_proxy_type)

        socks.set_default_proxy(
            proxy_type=proxy_type, addr=q.socks_proxy_host, port=q.socks_proxy_port
        )
        socket.socket = socks.socksocket

    email_box_type = helpdesk_settings.QUEUE_EMAIL_BOX_TYPE or q.email_box_type

    mail_defaults = {
        "pop3": {
            "ssl": {
                "port": 995,
                "init": poplib.POP3_SSL,
            },
            "insecure": {
                "port": 110,
                "init": poplib.POP3,
            },
            "sync": pop3_sync,
        },
        "imap": {
            "ssl": {
                "port": 993,
                "init": imaplib.IMAP4_SSL,
            },
            "insecure": {
                "port": 143,
                "init": imaplib.IMAP4,
            },
            "sync": imap_sync,
        },
        "oauth": {
            "ssl": {
                "port": 993,
                "init": imaplib.IMAP4_SSL,
            },
            "insecure": {
                "port": 143,
                "init": imaplib.IMAP4,
            },
            "sync": imap_oauth_sync,
        },
    }
    if email_box_type in mail_defaults:
        encryption = "insecure"
        if q.email_box_ssl or helpdesk_settings.QUEUE_EMAIL_BOX_SSL:
            encryption = "ssl"
        if not q.email_box_port:
            q.email_box_port = mail_defaults[email_box_type][encryption]["port"]

        server = mail_defaults[email_box_type][encryption]["init"](
            q.email_box_host or helpdesk_settings.QUEUE_EMAIL_BOX_HOST,
            int(q.email_box_port),
        )
        logger.info(f"Attempting {email_box_type.upper()} server login")
        mail_defaults[email_box_type]["sync"](q, logger, server)

    elif email_box_type == "local":
        mail_dir = q.email_box_local_dir or "/var/lib/mail/helpdesk/"
        mail = [
            join(mail_dir, f) for f in os.listdir(mail_dir) if isfile(join(mail_dir, f))
        ]
        logger.info(f"Found {len(mail)} messages in local mailbox directory")

        logger.info(f"Found {len(mail)} messages in local mailbox directory")
        for i, m in enumerate(mail, 1):
            logger.info(f"Processing message {i}")
            with open(m, "r") as f:
                full_message = encoding.force_str(f.read(), errors="replace")
                try:
                    ticket = extract_email_metadata(
                        message=full_message, queue=q, logger=logger
                    )
                except IgnoreTicketException:
                    logger.warning(
                        "Message %d was ignored and will be left in local directory", i
                    )
                except DeleteIgnoredTicketException:
                    os.unlink(m)
                    logger.warning(
                        "Message %d was ignored and deleted local directory", i
                    )
                else:
                    if ticket:
                        logger.info(
                            "Successfully processed message %d, ticket/comment created.",
                            i,
                        )
                        try:
                            # delete message file if ticket was successful
                            os.unlink(m)
                        except OSError as e:
                            logger.error("Unable to delete message %d (%s).", i, str(e))
                        else:
                            logger.info("Successfully deleted message %d.", i)
                    else:
                        logger.warning(
                            "Message %d was not successfully processed, and will be left in local directory",
                            i,
                        )
