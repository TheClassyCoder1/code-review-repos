package com.example.lending.ops.part2.k8s;

/**
 * Admits only plain YAML: no {@code !tag} and no {@code %TAG} directive, so a loader can only build
 * maps, lists and scalars, never an arbitrary Java type. Pod templates never need tags, so any
 * {@code !} is refused outright; the size cap keeps alias expansion small.
 */
public final class YamlTagScreen {

    public static String requirePlainYaml(String content) {
        if (content == null || content.length() > 256 * 1024 || content.indexOf('!') >= 0 || content.contains("%TAG")) {
            throw new IllegalArgumentException("pod template must be plain YAML without tags, at most 256 KiB");
        }
        return content;
    }
}
