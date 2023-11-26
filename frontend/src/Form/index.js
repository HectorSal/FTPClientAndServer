import React from 'react';
import { Button, Input, Row, Col } from 'reactstrap';

const FTPClient = () => {
  return (
    <div className="container mx-auto p-4">
      <ConnectionForm />
      <div className="flex mt-4">
        <FileExplorer title="Local files" />
        <TransferControls />
        <FileExplorer title="Remote files" />
      </div>
    </div>
  );
};

const ConnectionForm = () => {
  return (
    <Row className='mb-[10px] t-justify-center t-flex'>
      <Col xs={2}>
        <Input type="text" placeholder="Host" className='p-[5px] mr-[4px] w-[15px]' />
      </Col>
      <Col xs={1}>
        <Input type="text" placeholder="Port" className='p-[5px] mr-[2px] w-[15px]' />
      </Col>
      <Col xs={3}>
        <Input type="text" placeholder="Username" className='mr-[2px] w-[25px]' />
      </Col>
      <Col xs={3}>
        <Input type="text" placeholder="Password" className='p-[5px] mr-[2px] w-[15px]' />
      </Col>
      <Col xs={3}>
        <Button color='primary'>Connect</Button>
      </Col>
      
    </Row>
  );
};


const FileExplorer = ({ title }) => {
  return (
    <div className="flex-1 mr-2 p-4 border border-gray-300">
      <div className="bg-gray-200 p-2 mb-2 text-sm font-mono">{title}</div>
      {/* List of files and directories would be rendered here */}
    </div>
  );
};

const TransferControls = () => {
  return (
    <div className="flex flex-col justify-center px-4">
      <Button className="mb-2" color="success">{'>'}</Button>
      <Button color="info">{'<'}</Button>
    </div>
  );
};

export default FTPClient;

