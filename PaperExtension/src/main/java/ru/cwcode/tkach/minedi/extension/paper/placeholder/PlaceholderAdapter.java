package ru.cwcode.tkach.minedi.extension.paper.placeholder;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import revxrsal.asm.MethodCaller;
import ru.cwcode.cwutils.collections.CollectionUtils;
import ru.cwcode.cwutils.text.StringToObjectParser;
import ru.cwcode.tkach.minedi.extension.paper.PaperExtension;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlaceholderAdapter {
  private final PaperExtension extension;
  private final Map<String, List<MethodData>> placeholders = new HashMap<>();
  
  public PlaceholderAdapter(PaperExtension extension) {
    this.extension = extension;
  }
  
  public void register(Object source, Method method, String identifier) {
    placeholders.computeIfAbsent(identifier, s -> new ArrayList<>())
                .add(new MethodData(method.getParameterTypes(), MethodCaller.wrap(method).bindTo(source)));
    
    extension.addDelayedTask(() -> new PlaceholderExpansion() {
      @Override
      public @NotNull String getIdentifier() {
        return identifier;
      }
      
      @Override
      public String getRequiredPlugin() {
        return extension.getPlugin().getName();
      }
      
      @Override
      public @NotNull String getAuthor() {
        return CollectionUtils.toString(extension.getPlugin().getDescription().getAuthors());
      }
      
      @Override
      public @NotNull String getVersion() {
        return extension.getPlugin().getDescription().getVersion();
      }
      
      @Override
      public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        return PlaceholderAdapter.this.onRequest(getIdentifier(), player, params);
      }
    }.register());
  }
  
  protected String onRequest(String identifier, @Nullable OfflinePlayer player, String params) {
    try {
      for (MethodData methodData : placeholders.get(identifier)) {
        String[] strParameters = params.split("_");
        
        Object[] adapt = adapt(player, methodData.parameters(), strParameters);
        if (adapt == null) continue;
        
        Object call = methodData.boundMethodCaller().call(adapt);
        if (call == null) return null;
        
        return call.toString();
      }
    } catch (Exception e) {
      e.printStackTrace();
      return e.getMessage();
    }
    return null;
  }
  
  protected Object[] adapt(@Nullable OfflinePlayer player, Class<?>[] types, String[] strParameters) {
    if (types.length < strParameters.length + 1) return null;
    
    int strParamIndex = 0;
    Object[] adapted = new Object[types.length];
    
    for (int i = 0; i < types.length; i++) {
      Class<?> clazz = types[i];
      if (OfflinePlayer.class.isAssignableFrom(clazz)) {
        adapted[i] = player;
        continue;
      }
      
      if (strParameters.length == strParamIndex) continue;
      String strParameter = strParameters[strParamIndex++];
      
      Object parsed = StringToObjectParser.parse(strParameter, clazz);
      if (parsed == null) {
        throw new IllegalArgumentException("Unsupported type: " + clazz);
      }
      
      adapted[i] = parsed;
    }
    
    return adapted;
  }
}
