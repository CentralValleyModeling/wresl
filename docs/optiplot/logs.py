import logging


def get_logger(name: str):
    logger = logging.getLogger(name)
    logger.setLevel(logging.INFO)

    ch = logging.StreamHandler()
    ch.setLevel(logging.DEBUG)

    formatter = logging.Formatter(
        "%(asctime)s - %(levelname)5s - %(name)s - %(message)s"
    )

    ch.setFormatter(formatter)
    logger.addHandler(ch)

    return logger
