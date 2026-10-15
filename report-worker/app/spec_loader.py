import yaml


class SpecLoader(yaml.SafeLoader):
    """Safe YAML loader with a !cents tag for money literals."""


def _cents_constructor(loader: yaml.SafeLoader, node: yaml.Node) -> int:
    value = loader.construct_scalar(node)
    return int(round(float(value) * 100))


SpecLoader.add_constructor("!cents", _cents_constructor)
