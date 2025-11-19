package com.bentahsin.configuration.converter;

public interface Converter<S, T> {
    /**
     * Config dosyasından gelen veriyi (S), sınıf değişkenine (T) çevirir.
     * @param source Config'den gelen ham veri (örn: "15m")
     * @return Değişkene atanacak veri (örn: 900L)
     */
    T convertToField(S source);

    /**
     * Sınıf değişkenindeki veriyi (T), Config dosyasına kaydedilecek formata (S) çevirir.
     * @param source Sınıftaki veri (örn: 900L)
     * @return Config'e yazılacak veri (örn: "15m")
     */
    S convertToConfig(T source);
}