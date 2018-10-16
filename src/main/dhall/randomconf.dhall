{-

Generate a large i18n file

` $ dhall-to-json --pretty  <<< "./randomconf.dhall"

-}

let generate =
    https://prelude.dhall-lang.org/List/generate

in let map =
    https://prelude.dhall-lang.org/List/map

in let buildEntry =
   λ(prefix: Text) ->
   λ(i: Natural) -> { mapKey   = "key${Natural/show i}",
                      mapValue = "${prefix}value${Natural/show i}" }

in let KeyValue =
    { mapKey:   Text,
      mapValue: Text }

in let NKeyValue =
    { mapKey:   Text,
      mapValue: List KeyValue }

in let makeLang =
    λ(count: Natural) ->
    λ(lang: Text) -> { mapKey   = lang,
                       mapValue = generate count KeyValue (buildEntry lang) }

in let langs=
    ["fr", "en", "de", "it", "es"]

in let keyCount =
    3000

in map Text NKeyValue (makeLang keyCount) langs