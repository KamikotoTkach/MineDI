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
import java.util.*;

public class PlaceholderAdapter {
  private final PaperExtension extension;
  private final Map<String, List<MethodData>> placeholders = new HashMap<>();
  
  public PlaceholderAdapter(PaperExtension extension) {
    this.extension = extension;
  }
  
  public void register(Object source, Method method, String identifier) {
    method.setAccessible(true);
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
    int hasPlayer = types.length > 0 && OfflinePlayer.class.isAssignableFrom(types[0]) ? 1 : 0;
    
    if (types.length == 1 + hasPlayer && types[hasPlayer].equals(String.class.arrayType())) {
      return hasPlayer > 0 ? new Object[]{player, strParameters} : new Object[]{strParameters};
    }
    
    if (types.length < strParameters.length + hasPlayer) return null;
    
    Object[] adapted = new Object[types.length];
    
    if (hasPlayer > 0) {
      adapted[0] = player;
    }
    
    for (int i = hasPlayer; i < types.length; i++) {
      Class<?> clazz = types[i];
      
      if (strParameters.length == i - hasPlayer) break;
      
      String strParameter = strParameters[i-hasPlayer];
      
      Object parsed = StringToObjectParser.parse(strParameter, clazz);
      if (parsed == null) {
        throw new IllegalArgumentException("Unsupported type: " + clazz);
      }
      
      adapted[i] = parsed;
    }
    
    return adapted;
  }
  
}
