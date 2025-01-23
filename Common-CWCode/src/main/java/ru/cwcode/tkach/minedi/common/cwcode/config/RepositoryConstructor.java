package ru.cwcode.tkach.minedi.common.cwcode.config;

import ru.cwcode.tkach.config.repository.Repository;
import ru.cwcode.tkach.config.repository.RepositoryManager;
import ru.cwcode.tkach.minedi.DiApplication;
import ru.cwcode.tkach.minedi.constructor.BeanConstructor;
import ru.cwcode.tkach.minedi.data.BeanData;

@SuppressWarnings("rawtypes")
public class RepositoryConstructor implements BeanConstructor {
  final RepositoryManager repositoryManager;
  
  public RepositoryConstructor(RepositoryManager repositoryManager) {
    this.repositoryManager = repositoryManager;
  }
  
  @Override
  public <T> T construct(Class<T> clazz, BeanData data, DiApplication application) {
    if (!Repository.class.isAssignableFrom(clazz)) return null;
    
    return (T) repositoryManager.getRepository(clazz);
  }
}
