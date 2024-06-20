package services.tmitocar.repository;

import services.tmitocar.model.TmitocarFiles;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FilesRepository  extends MongoRepository<TmitocarFiles,ObjectId>{
    
}
